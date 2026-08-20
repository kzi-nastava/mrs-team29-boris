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
import java.util.function.Function;

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
    private final SimpMessagingTemplate messagingTemplate;
    private final AppNotificationService notificationService;
    private final RoutingService routingService;
    private final IdleVehiclePositionService idleVehiclePositionService;

    //Other
    private static final double TRACKING_SIMULATION_SPEED = 10.0;
    private static final double FINISH_PROGRESS_THRESHOLD = 0.999;

    // Ride creation
    @Override
    @Transactional
    public RideResponseDTO createRide(CreateRideRequestDTO request) {
        if (request.getOrigin().getLongitude().equals(request.getDestination().getLongitude())
                && request.getOrigin().getLatitude().equals(request.getDestination().getLatitude())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Polazište i odredište ne mogu biti ista tačka");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getScheduledTime() == null) request.setScheduledTime(now);

        if (request.getScheduledTime().isAfter(now.plusHours(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vožnja se može zakazati najviše pet sati unapred");
        }
        if (request.getScheduledTime().isBefore(now.minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vreme vožnje ne može biti u prošlosti");
        }

        // Find the passenger who created the ride
        Passenger creator = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Putnik nije pronađen"));

        // Check if creator is maybe blocked
        if (creator.isBlocked()) {
            String reason = creator.getBlockReason() == null || creator.getBlockReason().isBlank()
                    ? "Administrator nije uneo napomenu."
                    : creator.getBlockReason();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Nalog je blokiran. Razlog: " + reason);
        }
        if (rideRepository.existsStartedRideForPassenger(creator.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ne možete poručiti novu vožnju dok je trenutna vožnja u toku.");
        }

        // The backend calculates the authoritative road route. Distance, duration
        // and price are never trusted from the Android request.
        RoutingResult routing = routingService.calculate(
                request.getOrigin(), request.getStops(), request.getDestination());

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
        route.setDistance(routing.distanceKm());
        route.setDuration(routing.durationMinutes());
        route.setGeometry(new ArrayList<>(routing.geometry()));
        routeRepository.save(route);

        // Find suitable driver for ride
        List<Driver> drivers = driverRepository.filterAvailableDrivers(DriverStatus.ACTIVE ,request.isBabyFriendly(), request.isPetFriendly(), request.getVehicleType());
        Driver driver = findDriver(drivers, request.getScheduledTime(),
                routing.durationMinutes(), request.getOrigin());

        // Create ride
        Ride ride = new Ride();
        ride.setStatus(RideStatus.CREATED);
        ride.setScheduledTime(request.getScheduledTime());
        ride.setStops(createStops(request.getStops()));
        ride.setRoute(route);
        ride.setBabyFriendly(request.isBabyFriendly());
        ride.setPetFriendly(request.isPetFriendly());
        applyPriceSnapshot(ride, request.getVehicleType(), routing.distanceKm());

        // Linked passengers
        List<String> linkedEmails = normalizeLinkedEmails(
                request.getPassengerEmails(), creator.getEmail());
        List<Passenger> registeredPassengers = resolvePassengers(linkedEmails);
        ride.setPassengers(registeredPassengers); // Set linked passengers
        ride.setLinkedPassengerEmails(linkedEmails);

        // Save the passenger who created the ride
        ride.setRideCreator(creator);

        // Assign driver to ride
        if (driver == null) { // There is no available driver at the moment
            ride.setStatus(RideStatus.FAILED); // later send notification
            rideRepository.save(ride);
            notificationService.notify(creator, ride, "RIDE_REJECTED",
                    "Poručivanje vožnje nije uspelo: nema dostupnog vozača koji odgovara "
                            + "izabranom tipu vozila i zahtevima prevoza.",
                    "ride:" + ride.getId() + ":rejected:" + creator.getId());
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
            if (driver.getScheduledRides() == null) driver.setScheduledRides(new ArrayList<>());
            driver.getScheduledRides().add(ride);
            rideRepository.save(ride);

            notificationService.notify(driver, ride, "NEW_RIDE",
                    "Dodeljena vam je nova vožnja od " + origin.getAddress()
                            + " do " + destination.getAddress() + ".",
                    "ride:" + ride.getId() + ":assigned-driver:" + driver.getId());
            notificationService.notify(creator, ride, "RIDE_ACCEPTED",
                    "Vožnja je prihvaćena i vozač je dodeljen.",
                    "ride:" + ride.getId() + ":accepted:" + creator.getId());

            // Using websockets update drivers scheduled list
            messagingTemplate.convertAndSend(
                    "/topic/driver/" + driver.getId() + "/rides",
                    Map.of("action", "NEW_RIDE", "timestamp", LocalDateTime.now().toString())
            );

            // Send notifications and emails to linked passengers for this ride
            processNotifications(linkedEmails, ride, creator);

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
            passengerRepository.findByEmail(email.trim()).ifPresent(passenger -> {
                boolean alreadyAdded = passengers.stream().anyMatch(existing ->
                        existing.getId().equals(passenger.getId()));
                if (!alreadyAdded) passengers.add(passenger);
            });
        }
        return passengers;
    }

    private static List<String> normalizeLinkedEmails(List<String> emails, String creatorEmail) {
        if (emails == null) return new ArrayList<>();
        return emails.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .filter(email -> !email.equalsIgnoreCase(creatorEmail))
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    // Helper for sending notifications and emails to linked passengers
    private void processNotifications(List<String> emails, Ride ride, Passenger creator) {
        if (emails == null) return;

        for (String email : emails) {
            String normalizedEmail = email.trim();
            Optional<Passenger> passenger = passengerRepository.findByEmail(normalizedEmail);
            String link = trackingLink(ride);
            String mailBody = """
                    Poštovani,

                    Dodati ste na prihvaćenu vožnju #%d od %s do %s.
                    Vožnju možete pratiti otvaranjem linka na telefonu.

                    ClickAndDrive
                    """.formatted(ride.getId(), ride.getRoute().getOrigin().getAddress(),
                    ride.getRoute().getDestination().getAddress());

            if (passenger.isPresent()) {
                Passenger p = passenger.get();
                if (!p.getId().equals(creator.getId())) {
                    notificationService.notify(p, ride, "LINKED_RIDE",
                            creator.getName() + " " + creator.getSurname()
                                    + " vas je povezao sa prihvaćenom vožnjom #"
                                    + ride.getId() + ". Dodirnite za praćenje.",
                            "ride:" + ride.getId() + ":linked:" + p.getId());
                    emailService.sendRideTrackingEmail(p.getEmail(),
                            "Dodati ste na vožnju #" + ride.getId(), mailBody, link);
                }
            } else {
                // Neregistrovani primalac dobija samo email, kako specifikacija zahteva.
                emailService.sendRideTrackingEmail(normalizedEmail,
                        "Dodati ste na vožnju #" + ride.getId(), mailBody, link);
            }
        }
    }

    // Find suitable driver
    private Driver findDriver(List<Driver> drivers, LocalDateTime scheduledTime,
                              int durationMinutes, LocationDTO origin) {
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
            boolean activeConflict = false;
            boolean finishingSoon = false;

            // Check currently active ride
            Ride active = d.getActiveRide();
            if (active == null && d.getVehicle() != null
                    && Boolean.TRUE.equals(d.getVehicle().getBusy())) {
                // A vehicle marked busy without a regular active ride may belong
                // to a guest ride or stale external process; never assign it as free.
                continue;
            }
            if (active != null) {
                // When is currently active ride ending
                LocalDateTime activeStart = active.getStartTime() == null
                        ? active.getScheduledTime() : active.getStartTime();
                LocalDateTime activeEnd = activeStart.plusMinutes(active.getRoute().getDuration());

                // Overlap
                if (isOverlapping(requestStart, requestEnd, active.getScheduledTime(), activeEnd)) {
                    activeConflict = true;

                    // If driver is finishing within 10 minutes
                    if (isRideRequestedForNow(requestStart)) {
                        LocalDateTime now = LocalDateTime.now();
                        if (activeEnd.isAfter(now) && activeEnd.isBefore(now.plusMinutes(10))) {
                            // Busy but is finishing soon
                            finishingSoon = true;
                        }
                    }
                }
            }
            // Scheduled rides
            boolean scheduledConflict = false;
            for (Ride scheduled : safe(d.getScheduledRides())) {
                LocalDateTime scheduledEnd = scheduled.getScheduledTime()
                        .plusMinutes(scheduled.getRoute().getDuration());
                if (isOverlapping(requestStart, requestEnd,
                        scheduled.getScheduledTime(), scheduledEnd)) {
                    scheduledConflict = true;
                    break;
                }
            }
            if (scheduledConflict) continue;
            if (!activeConflict) {
                perfectCandidates.add(d);
            } else if (finishingSoon) {
                busyButFinishingCandidates.add(d);
            }
        }

        if (!perfectCandidates.isEmpty()) {
            return nearestByRoad(perfectCandidates, driver -> driver.getVehicle() == null
                    ? null : idleVehiclePositionService.currentLocation(driver.getVehicle()), origin);
        }

        if (!busyButFinishingCandidates.isEmpty()) {
            return nearestByRoad(busyButFinishingCandidates,
                    driver -> locationDto(driver.getActiveRide().getRoute().getDestination()), origin);
        }
        // No available drivers
        return null;
    }

    private Driver nearestByRoad(List<Driver> candidates,
                                 Function<Driver, LocationDTO> locationProvider,
                                 LocationDTO origin) {
        List<Driver> located = new ArrayList<>();
        List<LocationDTO> candidateLocations = new ArrayList<>();
        for (Driver driver : candidates) {
            LocationDTO location = locationProvider.apply(driver);
            if (location != null) {
                located.add(driver);
                candidateLocations.add(location);
            }
        }
        if (located.isEmpty()) return candidates.get(0);
        try {
            List<Double> distances = routingService.roadDistancesToDestination(
                    candidateLocations, origin);
            if (distances.size() != located.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Routing servis nije vratio udaljenost za sva vozila");
            }
            int bestIndex = 0;
            for (int i = 1; i < distances.size(); i++) {
                if (distances.get(i) < distances.get(bestIndex)) bestIndex = i;
            }
            return located.get(bestIndex);
        } catch (ResponseStatusException exception) {
            return located.stream()
                    .min(Comparator.comparingDouble(driver -> distanceToOrigin(
                            candidateLocations.get(located.indexOf(driver)), origin)))
                    .orElse(located.get(0));
        }
    }

    private static LocationDTO locationDto(Location location) {
        return location == null ? null : new LocationDTO(location.getLongitude(),
                location.getLatitude(), location.getAddress());
    }

    // Time interval overlap
    private boolean isOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean isRideRequestedForNow(LocalDateTime scheduledTime) {
        return scheduledTime.isBefore(LocalDateTime.now().plusMinutes(5));
    }

    private int calculateDriverWorkMinutes(Driver driver) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24Hours = now.minusHours(24);
        int totalMinutes = 0;

        for (Ride ride : rideRepository.findAllByDriverId(driver.getId())) {
            totalMinutes += overlapMinutes(ride.getStartTime(), ride.getEndTime(),
                    last24Hours, now);
        }
        for (GuestRide ride : guestRideRepository.findAllByDriverId(driver.getId())) {
            totalMinutes += overlapMinutes(ride.getStartTime(), ride.getEndTime(),
                    last24Hours, now);
        }
        return totalMinutes;
    }

    private static int overlapMinutes(LocalDateTime start, LocalDateTime end,
                                      LocalDateTime cutoff, LocalDateTime now) {
        if (start == null) return 0;
        LocalDateTime effectiveStart = start.isBefore(cutoff) ? cutoff : start;
        LocalDateTime effectiveEnd = end == null || end.isAfter(now) ? now : end;
        return effectiveEnd.isAfter(effectiveStart)
                ? (int) Duration.between(effectiveStart, effectiveEnd).toMinutes() : 0;
    }

    private static double distanceToOrigin(LocationDTO location, LocationDTO origin) {
        if (location == null) return Double.MAX_VALUE;
        double lat1 = Math.toRadians(location.getLatitude());
        double lat2 = Math.toRadians(origin.getLatitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(origin.getLongitude() - location.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
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
    public void startRide(Long rideId, boolean isGuest, String driverEmail) {
        if (isGuest) {
            GuestRide guestRide = guestRideRepository.findById(rideId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Vožnja nije pronađena."));
            Driver driver = assertAssignedDriver(guestRide.getDriver(), driverEmail);
            assertRideCanStart(guestRide.getStatus(), driver);
            guestRide.setStatus(RideStatus.STARTED);
            guestRide.setStartTime(LocalDateTime.now());
            guestRideRepository.save(guestRide);

            Vehicle vehicle = driver.getVehicle();
            vehicle.setBusy(true);
            vehicleRepository.save(vehicle);
        } else {
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Vožnja nije pronađena."));
            Driver driver = assertAssignedDriver(ride.getDriver(), driverEmail);
            assertRideCanStart(ride.getStatus(), driver);
            ride.setStatus(RideStatus.STARTED);
            ride.setStartTime(LocalDateTime.now());
            rideRepository.save(ride);

            driver.setActiveRide(ride);
            driverRepository.save(driver);

            Vehicle vehicle = driver.getVehicle();
            vehicle.setBusy(true);
            vehicleRepository.save(vehicle);

            notifyRideStarted(ride);
        }
    }

    private Driver assertAssignedDriver(Driver assignedDriver, String driverEmail) {
        if (assignedDriver == null || driverEmail == null
                || !assignedDriver.getEmail().equalsIgnoreCase(driverEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Samo vozač kome je vožnja dodeljena može da je započne.");
        }
        return assignedDriver;
    }

    private void assertRideCanStart(RideStatus status, Driver driver) {
        if (status != RideStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Samo zakazana vožnja može da se započne.");
        }
        if (driver.getActiveRide() != null
                || driver.getVehicle() == null
                || Boolean.TRUE.equals(driver.getVehicle().getBusy())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vozač već ima aktivnu vožnju.");
        }
    }

    private void notifyRideStarted(Ride ride) {
        Passenger creator = ride.getRideCreator();
        if (creator != null) {
            notificationService.notify(creator, ride, "RIDE_STARTED",
                    "Vožnja #" + ride.getId() + " je započeta.",
                    "ride:" + ride.getId() + ":started:" + creator.getId());
        }
        for (Passenger passenger : safe(ride.getPassengers())) {
            if (creator == null || !passenger.getId().equals(creator.getId())) {
                notificationService.notify(passenger, ride, "RIDE_STARTED",
                        "Vožnja #" + ride.getId() + " je započeta.",
                        "ride:" + ride.getId() + ":started:" + passenger.getId());
            }
        }
    }

    @Override
    @Transactional
    public RideTrackingResponseDTO getRideTracking(Long rideId) {
        return getRideTracking(rideId, null);
    }

    @Override
    @Transactional
    public RideTrackingResponseDTO getRideTracking(Long rideId, User requester) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        if (requester != null) assertCanTrackRide(ride, requester);

        if (ride.getRoute() == null ||
                ride.getRoute().getOrigin() == null ||
                ride.getRoute().getDestination() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride route is missing");
        }

        int durationMinutes = ride.getRoute().getDuration() <= 0
                ? 1
                : ride.getRoute().getDuration();

        double progress = calculateTrackingProgress(ride, durationMinutes);

        RoutePoint currentPoint = pointAlongRoute(ride.getRoute(), progress);

        LocationDTO vehicleLocation = new LocationDTO(
                currentPoint.getLongitude(),
                currentPoint.getLatitude(),
                "Vehicle current location"
        );

        int eta = Math.max(
                0,
                (int) Math.ceil(durationMinutes * (1.0 - progress))
        );

        double progressPercent = Math.round(progress * 10000.0) / 100.0;

        boolean isCreator = requester instanceof Passenger passenger
                && ride.getRideCreator() != null
                && ride.getRideCreator().getId().equals(passenger.getId());
        boolean alreadyReviewed = isCreator && safe(ride.getReviews()).stream()
                .anyMatch(review -> review.getPassenger() != null
                        && review.getPassenger().getId().equals(ride.getRideCreator().getId()));
        LocalDateTime reviewDeadline = ride.getEndTime() == null
                ? null : ride.getEndTime().plusDays(3);
        boolean canReview = isCreator
                && ride.getStatus() == RideStatus.FINISHED
                && !alreadyReviewed
                && reviewDeadline != null
                && !LocalDateTime.now().isAfter(reviewDeadline);

        return new RideTrackingResponseDTO(
                ride.getId(),
                vehicleLocation,
                eta,
                ride.getStatus().name(),
                progressPercent,
                routeGeometry(ride.getRoute()),
                locationDto(ride.getRoute().getOrigin()),
                locationDto(ride.getRoute().getDestination()),
                safe(ride.getStops()).stream().map(RideServiceImpl::locationDto).toList(),
                ride.getPrice(),
                canReview,
                alreadyReviewed,
                reviewDeadline
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanTrackRide(Long rideId, User requester) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vožnja nije pronađena"));
        assertCanTrackRide(ride, requester);
    }

    private void assertCanTrackRide(Ride ride, User requester) {
        if (requester instanceof Administrator) return;
        if (requester instanceof Driver driver && ride.getDriver() != null
                && ride.getDriver().getId().equals(driver.getId())) return;
        if (requester instanceof Passenger passenger) {
            if (ride.getRideCreator() != null
                    && ride.getRideCreator().getId().equals(passenger.getId())) return;
            boolean linked = safe(ride.getPassengers()).stream()
                    .anyMatch(value -> value.getId().equals(passenger.getId()));
            if (linked) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Nemate pristup praćenju ove vožnje");
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

        if (ride.isDemoLoopingSimulation()) {
            return loopingDemoProgress(elapsedSeconds);
        }

        double simulatedElapsedSeconds = elapsedSeconds * TRACKING_SIMULATION_SPEED;
        long totalSeconds = Math.max(1, durationMinutes * 60L);

        return Math.min(1.0, Math.max(0.0, simulatedElapsedSeconds / totalSeconds));

    }

    static double loopingDemoProgress(long elapsedSeconds) {
        final long oneWaySeconds = 90L;
        long offset = Math.floorMod(elapsedSeconds, oneWaySeconds * 2L);
        return offset <= oneWaySeconds
                ? offset / (double) oneWaySeconds
                : (oneWaySeconds * 2L - offset) / (double) oneWaySeconds;
    }

    static RoutePoint pointAlongRoute(Route route, double progress) {
        List<RoutePoint> points = route.getGeometry();
        if (points == null || points.size() < 2) {
            return new RoutePoint(
                    interpolate(route.getOrigin().getLongitude(),
                            route.getDestination().getLongitude(), progress),
                    interpolate(route.getOrigin().getLatitude(),
                            route.getDestination().getLatitude(), progress));
        }
        double total = 0.0;
        double[] segments = new double[points.size() - 1];
        for (int i = 1; i < points.size(); i++) {
            segments[i - 1] = segmentDistance(points.get(i - 1), points.get(i));
            total += segments[i - 1];
        }
        if (total == 0.0) return points.get(0);
        double target = total * progress;
        double covered = 0.0;
        for (int i = 0; i < segments.length; i++) {
            if (covered + segments[i] >= target) {
                double segmentProgress = segments[i] == 0.0
                        ? 0.0 : (target - covered) / segments[i];
                RoutePoint from = points.get(i);
                RoutePoint to = points.get(i + 1);
                return new RoutePoint(
                        interpolate(from.getLongitude(), to.getLongitude(), segmentProgress),
                        interpolate(from.getLatitude(), to.getLatitude(), segmentProgress));
            }
            covered += segments[i];
        }
        return points.get(points.size() - 1);
    }

    private static List<LocationDTO> routeGeometry(Route route) {
        if (route.getGeometry() == null || route.getGeometry().isEmpty()) {
            return List.of(
                    new LocationDTO(route.getOrigin().getLongitude(),
                            route.getOrigin().getLatitude(), route.getOrigin().getAddress()),
                    new LocationDTO(route.getDestination().getLongitude(),
                            route.getDestination().getLatitude(), route.getDestination().getAddress()));
        }
        return route.getGeometry().stream()
                .map(point -> new LocationDTO(point.getLongitude(), point.getLatitude(), null))
                .toList();
    }

    private static double segmentDistance(RoutePoint first, RoutePoint second) {
        double lat1 = Math.toRadians(first.getLatitude());
        double lat2 = Math.toRadians(second.getLatitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(second.getLongitude() - first.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    private static double interpolate(double start, double end, double progress) {
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
        applyPendingDriverDeactivation(driver);
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
        applyPendingDriverDeactivation(driver);
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

        Driver driver = assertAssignedDriver(ride.getDriver(), driverEmail);
        if (ride.getStatus() != RideStatus.STARTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Samo započeta vožnja može da se završi.");
        }
        if (calculateTrackingProgress(ride,
                Math.max(1, ride.getRoute().getDuration())) < FINISH_PROGRESS_THRESHOLD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vožnja može da se završi tek kada vozilo stigne na odredište.");
        }

        ride.setStatus(RideStatus.FINISHED);
        ride.setEndTime(LocalDateTime.now());

        double completedDistance = finishDistance(
                ride.getDistanceKm(), ride.getRoute(), distance);
        updateDriverAndVehicleAfterRide(driver, completedDistance, ride.getRoute(), (dist, type) -> {
            ensurePriceSnapshot(ride, type);
            ride.setDistanceKm(dist);
            ride.setPrice(calculateSnapshotPrice(
                    dist, ride.getBasePriceAtBooking(), ride.getPricePerKmAtBooking()));
            ride.getRoute().setDistance(dist);
            rideRepository.save(ride);
            sendSummaryEmails(ride);
        });
    }

    private void finishGuestRide(Long rideId, String driverEmail, double distance) {
        GuestRide guestRide = guestRideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest ride not found"));

        Driver driver = assertAssignedDriver(guestRide.getDriver(), driverEmail);
        if (guestRide.getStatus() != RideStatus.STARTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Samo započeta vožnja može da se završi.");
        }
        if (guestRideProgress(guestRide) < FINISH_PROGRESS_THRESHOLD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vožnja može da se završi tek kada vozilo stigne na odredište.");
        }

        guestRide.setStatus(RideStatus.FINISHED);
        guestRide.setEndTime(LocalDateTime.now());

        //GuestRide specific logic, no emails
        Vehicle vehicle = driver.getVehicle();
        driver.setActiveRide(null);
        applyPendingDriverDeactivation(driver);
        vehicle.setBusy(false);
        vehicle.setLocation(guestRide.getRoute().getDestination());

        ensurePriceSnapshot(guestRide, vehicle.getType());
        double completedDistance = finishDistance(
                guestRide.getDistanceKm(), guestRide.getRoute(), distance);
        guestRide.setDistanceKm(completedDistance);
        guestRide.setPrice(calculateSnapshotPrice(completedDistance,
                guestRide.getBasePriceAtBooking(), guestRide.getPricePerKmAtBooking()));
        guestRide.getRoute().setDistance(completedDistance);

        driverRepository.save(driver);
        vehicleRepository.save(vehicle);
        guestRideRepository.save(guestRide);
    }

    private static double guestRideProgress(GuestRide ride) {
        if (ride.getStatus() != RideStatus.STARTED || ride.getStartTime() == null
                || ride.getRoute() == null) return 0.0;
        long elapsedSeconds = Math.max(0,
                Duration.between(ride.getStartTime(), LocalDateTime.now()).getSeconds());
        long totalSeconds = Math.max(1, ride.getRoute().getDuration() * 60L);
        return Math.min(1.0,
                elapsedSeconds * TRACKING_SIMULATION_SPEED / totalSeconds);
    }

    private static double finishDistance(double bookedDistance, Route route,
                                         double fallbackDistance) {
        if (bookedDistance > 0) return bookedDistance;
        if (route != null && route.getDistance() > 0) return route.getDistance();
        return Math.max(0, fallbackDistance);
    }

    // Helper
    private void updateDriverAndVehicleAfterRide(Driver driver, double distance, Route route,
                                                 java.util.function.BiConsumer<Double, VehicleType> extraLogic) {
        driver.setActiveRide(null);
        applyPendingDriverDeactivation(driver);
        driverRepository.save(driver);

        Vehicle vehicle = driver.getVehicle();
        vehicle.setBusy(false);
        vehicle.setLocation(route.getDestination());
        vehicleRepository.save(vehicle);

        extraLogic.accept(distance, vehicle.getType());
    }

    private void applyPendingDriverDeactivation(Driver driver) {
        if (driver.isDeactivateAfterRide()) {
            driver.setStatus(DriverStatus.INACTIVE);
            driver.setDeactivateAfterRide(false);
        }
    }

    private void applyPriceSnapshot(Ride ride, VehicleType vehicleType, double distance) {
        VehiclePrice price = currentVehiclePrice();
        ride.setVehicleTypeAtBooking(vehicleType);
        ride.setBasePriceAtBooking(basePrice(price, vehicleType));
        ride.setPricePerKmAtBooking(price.getPerKm());
        ride.setDistanceKm(distance);
        ride.setPrice(calculateSnapshotPrice(distance,
                ride.getBasePriceAtBooking(), ride.getPricePerKmAtBooking()));
    }

    private void ensurePriceSnapshot(Ride ride, VehicleType fallbackType) {
        if (ride.getBasePriceAtBooking() > 0 && ride.getPricePerKmAtBooking() > 0) return;
        applyPriceSnapshot(ride, fallbackType,
                ride.getRoute() == null ? 0 : ride.getRoute().getDistance());
    }

    private void ensurePriceSnapshot(GuestRide ride, VehicleType fallbackType) {
        if (ride.getBasePriceAtBooking() > 0 && ride.getPricePerKmAtBooking() > 0) return;
        VehiclePrice price = currentVehiclePrice();
        ride.setVehicleTypeAtBooking(fallbackType);
        ride.setBasePriceAtBooking(basePrice(price, fallbackType));
        ride.setPricePerKmAtBooking(price.getPerKm());
    }

    private VehiclePrice currentVehiclePrice() {
        return vehiclePriceRepository.findTopBy()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vehicle price not found"));
    }

    private static double basePrice(VehiclePrice price, VehicleType vehicleType) {
        return switch (vehicleType) {
            case LUXURY -> price.getLuxury();
            case VAN -> price.getVan();
            default -> price.getStandard();
        };
    }

    private static double calculateSnapshotPrice(double distance, double base, double perKm) {
        return (double) Math.round(base + distance * perKm);
    }

    private void sendSummaryEmails(Ride ride) {
        long minutes = Duration.between(ride.getStartTime(), ride.getEndTime()).toMinutes();

        String routeInfo = ride.getRoute().getOrigin().getAddress() + " -> " +
                ride.getRoute().getDestination().getAddress();

        String subject = "Ride Summary - " + ride.getId();
        String bodyTemplate = """
            Poštovani,

            Vaša vožnja je uspešno završena.

            Rezime:
            - Ruta: %s
            - Trajanje: %d minuta
            - Ukupna cena: %.2f RSD

            Putnik koji je poručio vožnju može da ostavi ocenu u roku od 3 dana
            otvaranjem ove vožnje u aplikaciji.

            ClickAndDrive
            """;

        String finalBody = String.format(bodyTemplate, routeInfo, minutes, ride.getPrice());

        LinkedHashSet<String> allEmails = new LinkedHashSet<>(
                safe(ride.getLinkedPassengerEmails()));
        for (Passenger passenger : safe(ride.getPassengers())) {
            allEmails.add(passenger.getEmail());
            notificationService.notify(passenger, ride, "RIDE_FINISHED",
                    "Vožnja #" + ride.getId() + " je uspešno završena.",
                    "ride:" + ride.getId() + ":finished:" + passenger.getId());
        }

        Passenger creator = ride.getRideCreator();
        allEmails.add(creator.getEmail());
        allEmails.forEach(email -> emailService.sendRideTrackingEmail(
                email, subject, finalBody, trackingLink(ride)));
        boolean creatorAlreadyLinked = safe(ride.getPassengers()).stream()
                .anyMatch(passenger -> passenger.getId().equals(creator.getId()));
        if (!creatorAlreadyLinked) {
            notificationService.notify(creator, ride, "RIDE_FINISHED",
                    "Vožnja #" + ride.getId() + " je uspešno završena.",
                    "ride:" + ride.getId() + ":finished:" + creator.getId());
        }
    }

    private static String trackingLink(Ride ride) {
        return "clickanddrive://ride-tracking?rideId=" + ride.getId();
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
                    false,
                    allRidePassengers(r).stream().map(passenger ->
                            new RidePassengerResponseDTO(
                                    passenger.getId(), passenger.getName(), passenger.getSurname(),
                                    passenger.getEmail(), passenger.getPhone(),
                                    passenger.getProfileImageUrl())).toList()
            ));
        }

        for (GuestRide gr : guestRides) {
            allRides.add(new ScheduledRideResponseDTO(
                    gr.getId(),
                    gr.getRoute().getOrigin().getAddress(),
                    gr.getRoute().getDestination().getAddress(),
                    gr.getScheduledTime(),
                    true,
                    List.of()
            ));
        }

        allRides.sort(Comparator.comparing(ScheduledRideResponseDTO::getScheduledTime));

        int start = Math.min((page - 1) * size, allRides.size());
        int end = Math.min(start + size, allRides.size());
        List<ScheduledRideResponseDTO> pagedList = allRides.subList(start, end);

        return new PageImpl<>(pagedList, pageable, allRides.size());
    }

    private static List<Passenger> allRidePassengers(Ride ride) {
        LinkedHashMap<Long, Passenger> passengers = new LinkedHashMap<>();
        if (ride.getRideCreator() != null) {
            passengers.put(ride.getRideCreator().getId(), ride.getRideCreator());
        }
        for (Passenger passenger : safe(ride.getPassengers())) {
            passengers.put(passenger.getId(), passenger);
        }
        return new ArrayList<>(passengers.values());
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
        dto.setVehicleTypeAtBooking(ride.getVehicleTypeAtBooking() == null
                ? null : ride.getVehicleTypeAtBooking().name());
        dto.setBasePriceAtBooking(ride.getBasePriceAtBooking());
        dto.setPricePerKmAtBooking(ride.getPricePerKmAtBooking());
        dto.setDistanceKm(ride.getDistanceKm() > 0 ? ride.getDistanceKm()
                : ride.getRoute() == null ? 0 : ride.getRoute().getDistance());

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
