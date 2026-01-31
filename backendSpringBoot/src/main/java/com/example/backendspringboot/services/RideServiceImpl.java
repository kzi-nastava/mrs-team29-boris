package com.example.backendspringboot.services;

import ch.qos.logback.core.CoreConstants;
import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.*;
import com.example.backendspringboot.dto.response.*;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import com.example.backendspringboot.services.interfaces.EmailService;
import com.example.backendspringboot.services.interfaces.RideService;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    //Repository
    private final RideRepository rideRepository;
    private final GuestRideRepository guestRideRepository;
    private final LocationRepository locationRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final PanicRepository panicRepository;
    private final PassengerRepository passengerRepository;
    private final InconsistencyReportRepository inconsistencyReportRepository;
    private final VehicleRepository vehicleRepository;
    //Service
    private final EmailService emailService;
    private final VehiclePriceRepository vehiclePriceRepository;

    //Other
    private static final double TRACKING_SIMULATION_SPEED = 10.0;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Ride creation
    @Override
    public RideResponseDTO createRide(CreateRideRequestDTO request) {
        // Validation
        if ((request.getOrigin().getLongitude().equals(request.getDestination().getLongitude())) &&
                (request.getOrigin().getLatitude().equals(request.getDestination().getLatitude())) &&
                (request.getOrigin().getAddress().equals(request.getDestination().getAddress()))) {
            try {
                throw new BadRequestException("Both origin and destination cannot be the same");
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        if (request.getScheduledTime().isAfter(now.plusHours(5))) {
            try {
                throw new BadRequestException("You cannot schedule a ride more than five hours in advance");
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }

        // Find the passenger who created the ride
        Passenger creator = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + request.getPassengerId()));

        // Check if creator is maybe blocked
        if (creator.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your are blocked! Reason " + creator.getBlockReason());
        }

        // Create origin Location
        Location origin = new Location();
        origin.setLongitude(request.getOrigin().getLongitude());
        origin.setLatitude(request.getOrigin().getLatitude());
        origin.setAddress(request.getOrigin().getAddress());
        locationRepository.save(origin);

        // Create destination Location
        Location destination = new Location();
        destination.setLongitude(request.getDestination().getLongitude());
        destination.setLatitude(request.getDestination().getLatitude());
        destination.setAddress(request.getDestination().getAddress());
        locationRepository.save(destination);

        // Create route
        Route route = new Route();
        route.setOrigin(origin);
        route.setDestination(destination);
        route.setDistance(request.getDistanceKm());
        route.setDuration(request.getDurationMinutes());
        routeRepository.save(route);

        // Find suitable driver for ride
        List<Driver> drivers = driverRepository.filterAvailableDrivers(DriverStatus.ACTIVE ,request.isBabyFriendly(), request.isPetFriendly(), request.getVehicleType());
        Driver driver = findDriver(drivers, request.getScheduledTime(), request.getDurationMinutes());

        // Create ride
        Ride ride = new Ride();
        ride.setStatus(RideStatus.CREATED);
        ride.setScheduledTime(request.getScheduledTime());
        ride.setStops(createStops(request.getStops()));
        ride.setRoute(route);
        ride.setBabyFriendly(request.isBabyFriendly());
        ride.setPetFriendly(request.isPetFriendly());

        // Linked passengers
        List<Passenger> registeredPassengers = resolvePassengers(request.getPassengerEmails());
        ride.setPassengers(registeredPassengers); // Set linked passengers

        // Save the passenger who created the ride
        ride.setRideCreator(creator);

        // Assign driver to ride
        if (driver == null) { // There is no available driver at the moment
            ride.setStatus(RideStatus.FAILED); // later send notification
            rideRepository.save(ride);
            // Map ride to response with status FAILED
            return new RideResponseDTO(
                    ride.getId(),
                    ride.getStatus(),
                    ride.getPrice()
            );
        }
        else { // An available driver was found
            ride.setStatus(RideStatus.SCHEDULED);
            ride.setDriver(driver);
            driver.getScheduledRides().add(ride);
            rideRepository.save(ride);

            // Using websockets update drivers scheduled list
            messagingTemplate.convertAndSend(
                    "/topic/driver/" + driver.getId() + "/rides",
                    Map.of("action", "NEW_RIDE", "timestamp", LocalDateTime.now().toString())
            );

            // Send notifications and emails to linked passengers for this ride
            processNotifications(request.getPassengerEmails(), ride);

            return new RideResponseDTO(
                    ride.getId(),
                    ride.getStatus(),
                    ride.getPrice()
            );
        }
    }

    // Create additional stops
    private List<Location> createStops(List<LocationDTO> incomingList) {
        if (incomingList == null) return null;

        List<Location> list = new ArrayList<>();
        for (LocationDTO loc : incomingList) {
            Location location = new Location();
            location.setLongitude(loc.getLongitude());
            location.setLatitude(loc.getLatitude());
            location.setAddress(loc.getAddress());
            locationRepository.save(location);
            list.add(location);
        }
        return list;
    }

    // Link other passengers
    // Find passengers in database if they are registered
    private List<Passenger> resolvePassengers(List<String> emails) {
        List<Passenger> passengers = new ArrayList<>();
        if (emails == null) return passengers;

        for (String email : emails) {
            passengerRepository.findByEmail(email).ifPresent(p -> {
                passengers.add(p);
            });
        }
        return passengers;
    }

    // Helper for sending notifications and emails to linked passengers
    private void processNotifications(List<String> emails, Ride ride) {
        if (emails == null) return;

        for (String email : emails) {
            Optional<Passenger> passenger = passengerRepository.findByEmail(email);

            if (passenger.isPresent()) {
                // This passenger is registered
                Passenger p = passenger.get();

                // SEND NOTIFICATION
                // ...
            }
            else {
                // This passenger is not registered
                // Send only mail
                EmailDetails emailToSend = new EmailDetails();
                emailToSend.setRecipient(email);
                emailToSend.setSubject("Ride in progress");
                emailToSend.setMsgBody(
                        "Hello " +  "\n\n" +
                                "Your are receiving this email because you've been linked to this drive:\n\n" +
                                "http://localhost:4200/map" + "\n\n" + // adjust the url to correct page later
                                "Have a safe ride.\n\n" +
                                "Best regards"
                );
                emailService.sendsSimpleMail(emailToSend);
            }
        }
    }

    // Find suitable driver
    private Driver findDriver(List<Driver> drivers, LocalDateTime scheduledTime, int durationMinutes) {
        LocalDateTime requestStart = scheduledTime;
        LocalDateTime requestEnd = scheduledTime.plusMinutes(durationMinutes);

        List<Driver> perfectCandidates = new ArrayList<>();
        List<Driver> busyButFinishingCandidates = new ArrayList<>();

        for (Driver d : drivers) {

            // Check if the driver is blocked
            if (d.isBlocked()) {
                continue; // Skip him
            }

            // Check work minutes
            int workMinutes = calculateDriverWorkMinutes(d);

            if (workMinutes > 480) {
                continue; // Skip driver consideration
            }
            boolean isBusy = false;

            // Check currently active ride
            Ride active = d.getActiveRide();
            if (active != null) {
                // When is currently active ride ending
                LocalDateTime activeEnd = active.getScheduledTime().plusMinutes(active.getRoute().getDuration());

                // Overlap
                if (isOverlapping(requestStart, requestEnd, active.getScheduledTime(), activeEnd)) {
                    isBusy = true;

                    // If driver is finishing within 10 minutes
                    if (isRideRequestedForNow(requestStart)) {
                        LocalDateTime now = LocalDateTime.now();
                        if (activeEnd.isAfter(now) && activeEnd.isBefore(now.plusMinutes(10))) {
                            // Busy but is finishing soon
                            busyButFinishingCandidates.add(d);
                        }
                    }
                }
            }
            // Scheduled rides
            if (!isBusy) {
                for (Ride scheduled : d.getScheduledRides()) {
                    LocalDateTime scheduledEnd = scheduled.getScheduledTime().plusMinutes(scheduled.getRoute().getDuration());

                    if (isOverlapping(requestStart, requestEnd, scheduled.getScheduledTime(), scheduledEnd)) {
                        isBusy = true;
                        break; // Busy, no need for further checking
                    }
                }
            }
            // Completely available
            if (!isBusy) {
                perfectCandidates.add(d);
            }
        }

        if (!perfectCandidates.isEmpty()) {
            // Add location check later...
            return perfectCandidates.get(0);
        }

        if (!busyButFinishingCandidates.isEmpty()) {
            // Add location check later...
            return busyButFinishingCandidates.get(0);
        }
        // No available drivers
        return null;
    }

    // Time interval overlap
    private boolean isOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean isRideRequestedForNow(LocalDateTime scheduledTime) {
        return scheduledTime.isBefore(LocalDateTime.now().plusMinutes(5));
    }

    private int calculateDriverWorkMinutes(Driver driver) {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        int totalMinutes = 0;

        for (Ride ride : driver.getFinishedRides()) {
            if (ride.getEndTime() != null && ride.getEndTime().isAfter(last24Hours)) {
                totalMinutes += ride.getRoute().getDuration();
            }
        }
        //...
        return totalMinutes;
    }

    @Override
    @Transactional
    public void cancelAnyRide(Long rideId, RideCancellationRequestDTO request) {

        if (request.isGuest()) {
            GuestRide guestRide = guestRideRepository.findById(rideId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest ride not found"));

            guestRide.setStatus(RideStatus.CANCELED);
            guestRide.setCancellationReason(request.getReason());
            guestRideRepository.save(guestRide);
            return;
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ride.setStatus(RideStatus.CANCELED);
        ride.setCancellationReason(request.getReason());
        ride.setCancelledBy(user);
        rideRepository.save(ride);
    }

    @Override
    public void panic(Long rideId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ride not found"
                ));

        if (ride.getStatus() == RideStatus.CANCELED ||
                ride.getStatus() == RideStatus.FINISHED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot trigger panic for finished or canceled ride"
            );
        }

        Panic panic = new Panic();
        panic.setRide(ride);
        panic.setCreatedAt(LocalDateTime.now());

        panicRepository.save(panic);
    }

    @Transactional
    public void startRide(Long rideId, boolean isGuest) {
        if (isGuest) {
            GuestRide guestRide = guestRideRepository.findById(rideId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GuestRide not found"));
            guestRide.setStatus(RideStatus.STARTED);
            guestRide.setStartTime(LocalDateTime.now());
            guestRideRepository.save(guestRide);

            //set vehicle and driver data
            Driver driver = guestRide.getDriver();
            ///TO-DO: can't set active ride because it's diff class
            Vehicle vehicle = driver.getVehicle();
            vehicle.setBusy(true);
            vehicleRepository.save(vehicle);
        } else {
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
            ride.setStatus(RideStatus.STARTED);
            ride.setStartTime(LocalDateTime.now());
            rideRepository.save(ride);

            //set vehicle and driver data
            Driver driver = ride.getDriver();
            driver.setActiveRide(ride);
            driverRepository.save(driver);

            Vehicle vehicle = driver.getVehicle();
            vehicle.setBusy(true);
            vehicleRepository.save(vehicle);
        }
    }

    @Override
    @Transactional
    public RideTrackingResponseDTO getRideTracking(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        if (ride.getRoute() == null ||
                ride.getRoute().getOrigin() == null ||
                ride.getRoute().getDestination() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride route is missing");
        }

        Location origin = ride.getRoute().getOrigin();
        Location destination = ride.getRoute().getDestination();

        int durationMinutes = ride.getRoute().getDuration() <= 0
                ? 1
                : ride.getRoute().getDuration();

        double progress = calculateTrackingProgress(ride, durationMinutes);

        double currentLongitude = interpolate(
                origin.getLongitude(),
                destination.getLongitude(),
                progress
        );

        double currentLatitude = interpolate(
                origin.getLatitude(),
                destination.getLatitude(),
                progress
        );

        LocationDTO vehicleLocation = new LocationDTO(
                currentLongitude,
                currentLatitude,
                "Vehicle current location"
        );

        int eta = Math.max(
                0,
                (int) Math.ceil(durationMinutes * (1.0 - progress))
        );

        double progressPercent = Math.round(progress * 10000.0) / 100.0;

        return new RideTrackingResponseDTO(
                ride.getId(),
                vehicleLocation,
                eta,
                ride.getStatus().name(),
                progressPercent
        );
    }

    private double calculateTrackingProgress(Ride ride, int durationMinutes) {
        if (ride.getStatus() == RideStatus.FINISHED ||
                ride.getStatus() == RideStatus.STOPPED) {
            return 1.0;
        }

        if (ride.getStatus() != RideStatus.STARTED || ride.getStartTime() == null) {
            return 0.0;
        }

        long elapsedSeconds = Duration.between(
                ride.getStartTime(),
                LocalDateTime.now()
        ).getSeconds();

        double simulatedElapsedSeconds = elapsedSeconds * TRACKING_SIMULATION_SPEED;
        long totalSeconds = Math.max(1, durationMinutes * 60L);

        return Math.min(1.0, Math.max(0.0, simulatedElapsedSeconds / totalSeconds));

    }

    private double interpolate(double start, double end, double progress) {
        return start + (end - start) * progress;
    }


    @Transactional
    public void stopRide(Long rideId, RideStopRequestDTO request) {

        if (request.getGuest()) {
            stopGuestRide(rideId, request);
        } else {
            stopRegularRide(rideId, request);
        }
    }

    private void stopRegularRide(Long rideId, RideStopRequestDTO dto) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getStatus() != RideStatus.STARTED) {
            throw new IllegalStateException("Ride is not started");
        }

        Location savedLocation = saveStopLocation(dto);

        Route route = ride.getRoute();
        route.setDestination(savedLocation);

        ride.setStatus(RideStatus.STOPPED);
        ride.setEndTime(LocalDateTime.now());

        rideRepository.save(ride);

        //update driver active ride
        Driver driver =  ride.getDriver();
        driver.setActiveRide(null);
        driverRepository.save(driver);

        //update vehicle
        Vehicle vehicle = driver.getVehicle();
        vehicle.setBusy(false);
        Location loc = new Location();
        loc.setLatitude(dto.getStopLocation().getLatitude());
        loc.setLongitude(dto.getStopLocation().getLongitude());
        loc.setAddress(dto.getStopLocation().getAddress());
        vehicle.setLocation(loc);
        vehicleRepository.save(vehicle);
    }

    private void stopGuestRide(Long rideId, RideStopRequestDTO dto) {

        GuestRide guestRide = guestRideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Guest ride not found"));

        if (guestRide.getStatus() != RideStatus.STARTED) {
            throw new IllegalStateException("Guest ride is not started");
        }

        Location savedLocation = saveStopLocation(dto);

        Route route = guestRide.getRoute();
        route.setDestination(savedLocation);

        guestRide.setStatus(RideStatus.STOPPED);
        guestRide.setEndTime(LocalDateTime.now());

        guestRideRepository.save(guestRide);

        //update driver active ride
        Driver driver =  guestRide.getDriver();
        driver.setActiveRide(null);
        driverRepository.save(driver);

        //update vehicle
        Vehicle vehicle = driver.getVehicle();
        vehicle.setBusy(false);
        Location loc = new Location();
        loc.setLatitude(dto.getStopLocation().getLatitude());
        loc.setLongitude(dto.getStopLocation().getLongitude());
        loc.setAddress(dto.getStopLocation().getAddress());
        vehicle.setLocation(loc);
        vehicleRepository.save(vehicle);
    }


    private Location saveStopLocation(RideStopRequestDTO dto) {
        Location loc = new Location();
        loc.setLatitude(dto.getStopLocation().getLatitude());
        loc.setLongitude(dto.getStopLocation().getLongitude());
        loc.setAddress(dto.getStopLocation().getAddress());
        return locationRepository.save(loc);
    }

    @Override
    @Transactional
    public void finishRide(Long rideId, String driverEmail, double distance, boolean isGuest) {
        if (isGuest) {
            finishGuestRide(rideId, driverEmail, distance);
        } else {
            finishRegularRide(rideId, driverEmail, distance);
        }
    }

    private void finishRegularRide(Long rideId, String driverEmail, double distance) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        if (ride.getStatus() != RideStatus.STARTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only started rides can be finished.");
        }

        ride.setStatus(RideStatus.FINISHED);
        ride.setEndTime(LocalDateTime.now());

        Driver driver = ride.getDriver();
        updateDriverAndVehicleAfterRide(driver, distance, ride.getRoute(), (dist, type) -> {
            double price = calculateRidePrice(dist, type);
            ride.setPrice(price);
            ride.getRoute().setDistance(dist);
            rideRepository.save(ride);
            sendSummaryEmails(ride);
        });
    }

    private void finishGuestRide(Long rideId, String driverEmail, double distance) {
        GuestRide guestRide = guestRideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest ride not found"));

        if (guestRide.getStatus() != RideStatus.STARTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only started guest rides can be finished.");
        }

        guestRide.setStatus(RideStatus.FINISHED);
        guestRide.setEndTime(LocalDateTime.now());

        Driver driver = guestRide.getDriver();

        //GuestRide specific logic, no emails
        Vehicle vehicle = driver.getVehicle();
        driver.setActiveRide(null);
        vehicle.setBusy(false);
        vehicle.setLocation(guestRide.getRoute().getDestination());

        double price = calculateRidePrice(distance, vehicle.getType());
        guestRide.setPrice(price);
        guestRide.getRoute().setDistance(distance);

        driverRepository.save(driver);
        vehicleRepository.save(vehicle);
        guestRideRepository.save(guestRide);
    }

    // Helper
    private void updateDriverAndVehicleAfterRide(Driver driver, double distance, Route route,
                                                 java.util.function.BiConsumer<Double, VehicleType> extraLogic) {
        driver.setActiveRide(null);
        driverRepository.save(driver);

        Vehicle vehicle = driver.getVehicle();
        vehicle.setBusy(false);
        vehicle.setLocation(route.getDestination());
        vehicleRepository.save(vehicle);

        extraLogic.accept(distance, vehicle.getType());
    }

    private double calculateRidePrice(double distance, VehicleType vehicleType) {
        double price = 0.0;
        VehiclePrice vehiclePrice = vehiclePriceRepository
                .findTopBy()
                .orElseThrow(() -> new RuntimeException("Vehicle price not found"));
        if (vehiclePrice == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle price not found");
        }
        switch (vehicleType) {
            case LUXURY -> price += vehiclePrice.getLuxury();
            case VAN ->  price += vehiclePrice.getVan();
            default -> price += vehiclePrice.getStandard();
        }
//        System.out.println("DEBUG: Racunam cenu za distancu: " + distance);

        price += distance * vehiclePrice.getPerKm();
        return (double) Math.round(price);

    }

    private void sendSummaryEmails(Ride ride) {
        long minutes = Duration.between(ride.getStartTime(), ride.getEndTime()).toMinutes();

        String routeInfo = ride.getRoute().getOrigin().getAddress() + " -> " +
                ride.getRoute().getDestination().getAddress();

        String subject = "Ride Summary - " + ride.getId();
        String bodyTemplate = """
            Dear Passenger,
            
            Your ride has been successfully finished.
            
            Summary:
            - Route: %s
            - Duration: %d minutes
            - Total Price: %.2f RSD
            
            Thank you for riding with us!
            """;

        String finalBody = String.format(bodyTemplate, routeInfo, minutes, ride.getPrice());

        for (Passenger passenger : ride.getPassengers()) {
            System.out.println("Email sent to " + passenger.getEmail());
            sendEmail(passenger.getEmail(), subject, finalBody);
        }

        String creatorEmail = ride.getRideCreator().getEmail();
        System.out.println("Email sent to " + creatorEmail);
        sendEmail(creatorEmail, subject, finalBody);
    }

    private void sendEmail(String to, String subject, String body) {
        EmailDetails details = new EmailDetails();
        details.setRecipient(to);
        details.setSubject(subject);
        details.setMsgBody(body);
        emailService.sendsSimpleMail(details);
    }

    @Override
    public InconsistencyReportResponseDTO reportInconsistency(
            Long rideId, InconsistencyReportRequestDTO dto, String passengerEmail) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        // started?
        if (ride.getStatus() != RideStatus.STARTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inconsistency can only be reported for active rides.");
        }

        // is passenger on the ride?
        boolean isCreator = ride.getRideCreator() != null
                && ride.getRideCreator().getEmail().equals(passengerEmail);

        boolean isLinkedPassenger = ride.getPassengers() != null
                && ride.getPassengers().stream()
                .anyMatch(p -> p.getEmail().equals(passengerEmail));

        boolean isPassenger = isCreator || isLinkedPassenger;

        if (!isPassenger) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a passenger in this ride.");
        }

        // report creation
        InconsistencyReport report = new InconsistencyReport();
        report.setNote(dto.getReason());
        report.setCreatedAt(LocalDateTime.now());
        report.setRide(ride);
        // set Passenger
        Passenger passenger = passengerRepository.findByEmail(passengerEmail).get();
        report.setPassenger(passenger);

        inconsistencyReportRepository.save(report);

        //mapping
        return new InconsistencyReportResponseDTO(
                report.getId(),
                report.getNote(),
                report.getCreatedAt(),
                passenger.getEmail(),
                ride.getId()
        );
    }

    @Override
    public Page<ScheduledRideResponseDTO> getDriverScheduledRides(Long driverId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size); // 0-based page

        List<Ride> rides = rideRepository.findAllByDriverId(driverId).stream()
                .filter(r -> r.getStatus() == RideStatus.SCHEDULED)
                .toList();

        List<GuestRide> guestRides = guestRideRepository.findAllByDriverId(driverId).stream()
                .filter(gr -> gr.getStatus() == RideStatus.SCHEDULED)
                .toList();

        List<ScheduledRideResponseDTO> allRides = new ArrayList<>();
        for (Ride r : rides) {
            allRides.add(new ScheduledRideResponseDTO(
                    r.getId(),
                    r.getRoute().getOrigin().getAddress(),
                    r.getRoute().getDestination().getAddress(),
                    r.getScheduledTime(),
                    false // false = regular ride
            ));
        }

        for (GuestRide gr : guestRides) {
            allRides.add(new ScheduledRideResponseDTO(
                    gr.getId(),
                    gr.getRoute().getOrigin().getAddress(),
                    gr.getRoute().getDestination().getAddress(),
                    gr.getScheduledTime(),
                    true // true = guest ride
            ));
        }

        allRides.sort(Comparator.comparing(ScheduledRideResponseDTO::getScheduledTime));

        int start = Math.min((page - 1) * size, allRides.size());
        int end = Math.min(start + size, allRides.size());
        List<ScheduledRideResponseDTO> pagedList = allRides.subList(start, end);

        return new PageImpl<>(pagedList, pageable, allRides.size());
    }

    @Override
    public PassengerRideDetailsResponseDTO getRideDetails(Long rideId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        PassengerRideDetailsResponseDTO dto = new PassengerRideDetailsResponseDTO();

        dto.setDriverEmail(ride.getDriver().getEmail());
        dto.setStartTime(ride.getStartTime());
        dto.setEndTime(ride.getEndTime());
        dto.setTotalPrice(ride.getPrice());

        dto.setStatus(ride.getStatus().name());

        if (ride.getDriver() != null) {
            dto.setDriverEmail(ride.getDriver().getEmail());
            dto.setDriverName(ride.getDriver().getName() + " " + ride.getDriver().getSurname());
        }

        if (ride.getRoute() != null) {
            Location start = ride.getRoute().getOrigin();
            Location end = ride.getRoute().getDestination();

            dto.setOrigin(new LocationDTO(
                    start.getLongitude(),
                    start.getLatitude(),
                    start.getAddress()
            ));

            dto.setDestination(new LocationDTO(
                    end.getLongitude(),
                    end.getLatitude(),
                    end.getAddress()
            ));
        }

        if (ride.getInconsistencyReports() != null) {
            dto.setInconsistencyReports(
                    ride.getInconsistencyReports()
                            .stream()
                            .map(r -> r.getNote())
                            .toList()
            );
        }

        dto.setPetFriendly(ride.isPetFriendly());
        dto.setBabyFriendly(ride.isBabyFriendly());

        if (ride.getReviews() != null && !ride.getReviews().isEmpty()) {
            Review review = ride.getReviews().get(0);
            dto.setDriverRating(review.getDriverRating());
            dto.setVehicleRating(review.getVehicleRating());
        } else {
            dto.setDriverRating(0);
            dto.setVehicleRating(0);
        }

        return dto;
    }
}
