package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.CompleteRegistrationRequestDTO;
import com.example.backendspringboot.dto.request.DriverRegistrationRequestDTO;
import com.example.backendspringboot.dto.response.DriverRegistrationResponseDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import com.example.backendspringboot.dto.response.VehicleResponseDTO;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.DriverStatus;
import com.example.backendspringboot.model.EmailDetails;
import com.example.backendspringboot.model.Vehicle;
import com.example.backendspringboot.dto.response.DriverRideHistoryResponseDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import com.example.backendspringboot.services.interfaces.DriverService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    // Inject repository
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final EmailServiceImpl emailService;
    private final PasswordEncoder passwordEncoder;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final GuestRideRepository guestRideRepository;

    @Override
    public List<DriverRideHistoryResponseDTO> getDriverRideHistory(
            Long driverId, LocalDate from, LocalDate to) {
        // --- SECURITY ---
        if (!isOwnerOrAdmin(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemate pristup ovim podacima.");
        }

        List<Ride> rides = rideRepository.findAllByDriverId(driverId);
        List<DriverRideHistoryResponseDTO> dtos = new ArrayList<>();

        for (Ride ride : rides) {
            if (isHistorical(ride.getStatus())
                    && isWithinDateRange(historyDate(ride.getStartTime(), ride.getCreatedAt()), from, to)) {
                dtos.add(getRideHistoryResponseDTO(ride));
            }
        }

        List<GuestRide>  guestRides = guestRideRepository.findAllByDriverId(driverId);
        for (GuestRide guestRide : guestRides) {
            if (isHistorical(guestRide.getStatus())
                    && isWithinDateRange(historyDate(guestRide.getStartTime(), guestRide.getCreatedAt()), from, to)) {
                dtos.add(getGuestRideHistoryResponseDTO(guestRide));
            }
        }
        dtos.sort(Comparator.comparing(DriverServiceImpl::dtoHistoryDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return dtos;
    }

    @Override
    public DriverRideHistoryResponseDTO getDriverRideHistoryDetail(
            Long driverId, Long rideId, boolean guest) {
        if (!isOwnerOrAdmin(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (guest) {
            GuestRide ride = guestRideRepository.findById(rideId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return getGuestRideHistoryResponseDTO(ride);
        }
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return getRideHistoryResponseDTO(ride);
    }

    private static DriverRideHistoryResponseDTO getRideHistoryResponseDTO(Ride ride) {
        DriverRideHistoryResponseDTO dto = new DriverRideHistoryResponseDTO();
        dto.setId(ride.getId());
        dto.setCreatedAt(ride.getCreatedAt());
        dto.setStartTime(ride.getStartTime());
        dto.setEndTime(ride.getEndTime());
        dto.setTotalPrice(ride.getPrice());
        dto.setVehicleTypeAtBooking(ride.getVehicleTypeAtBooking() == null
                ? null : ride.getVehicleTypeAtBooking().name());
        dto.setBasePriceAtBooking(ride.getBasePriceAtBooking());
        dto.setPricePerKmAtBooking(ride.getPricePerKmAtBooking());
        dto.setDistanceKm(ride.getDistanceKm() > 0 ? ride.getDistanceKm()
                : ride.getRoute() == null ? 0 : ride.getRoute().getDistance());
        dto.setPanicPressed(ride.isPanicPressed());

        dto.setStatus(ride.getStatus() == null ? null : ride.getStatus().name());
        dto.setCanceled(ride.getStatus() == RideStatus.CANCELED);
        dto.setCanceledBy(ride.getCancelledBy() == null ? null : ride.getCancelledBy().getEmail());
        dto.setCancellationReason(ride.getCancellationReason());
        dto.setGuest(false);
        List<Passenger> ridePassengers = ride.getPassengers() == null
                ? Collections.emptyList() : ride.getPassengers();
        dto.setPassengers(ridePassengers.stream().map(passenger ->
                new com.example.backendspringboot.dto.response.RidePassengerResponseDTO(
                        passenger.getId(), passenger.getName(), passenger.getSurname(),
                        passenger.getEmail(), passenger.getPhone(), passenger.getProfileImageUrl()))
                .toList());
        List<Review> reviews = ride.getReviews() == null ? Collections.emptyList() : ride.getReviews();
        dto.setReviews(reviews.stream().map(review ->
                new com.example.backendspringboot.dto.response.RideReviewResponseDTO(
                        review.getPassenger() == null ? null : review.getPassenger().getEmail(),
                        review.getDriverRating(), review.getVehicleRating(), review.getComment(),
                        review.getCreationTime())).toList());
        List<InconsistencyReport> reports = ride.getInconsistencyReports() == null
                ? Collections.emptyList() : ride.getInconsistencyReports();
        dto.setInconsistencyReports(reports.stream().map(report ->
                new com.example.backendspringboot.dto.response.InconsistencyReportResponseDTO(
                        report.getId(), report.getNote(), report.getCreatedAt(),
                        report.getPassenger() == null ? null : report.getPassenger().getEmail(),
                        ride.getId())).toList());

        if (ride.getRoute() != null) {
            Location start = ride.getRoute().getOrigin();
            Location end = ride.getRoute().getDestination();
            dto.setOrigin(new LocationDTO(start.getLongitude(), start.getLatitude(), start.getAddress()));
            dto.setDestination(new LocationDTO(end.getLongitude(), end.getLatitude(), end.getAddress()));
        }
        return dto;
    }

    private static DriverRideHistoryResponseDTO getGuestRideHistoryResponseDTO(GuestRide guestRide) {
        DriverRideHistoryResponseDTO dto = new DriverRideHistoryResponseDTO();
        dto.setId(guestRide.getId());
        dto.setCreatedAt(guestRide.getCreatedAt());
        dto.setStartTime(guestRide.getStartTime());
        dto.setEndTime(guestRide.getEndTime());
        dto.setTotalPrice(guestRide.getPrice());
        dto.setVehicleTypeAtBooking(guestRide.getVehicleTypeAtBooking() == null
                ? null : guestRide.getVehicleTypeAtBooking().name());
        dto.setBasePriceAtBooking(guestRide.getBasePriceAtBooking());
        dto.setPricePerKmAtBooking(guestRide.getPricePerKmAtBooking());
        dto.setDistanceKm(guestRide.getDistanceKm() > 0 ? guestRide.getDistanceKm()
                : guestRide.getRoute() == null ? 0 : guestRide.getRoute().getDistance());
        dto.setPanicPressed(guestRide.isPanicPressed());
        dto.setPassengers(Collections.emptyList());
        dto.setReviews(Collections.emptyList());
        dto.setInconsistencyReports(Collections.emptyList());
        dto.setStatus(guestRide.getStatus() == null ? null : guestRide.getStatus().name());
        dto.setCanceled(guestRide.getStatus() == RideStatus.CANCELED);
        dto.setCanceledBy(guestRide.getCancelledBy() == null
                ? null : guestRide.getCancelledBy().getEmail());
        dto.setCancellationReason(guestRide.getCancellationReason());
        dto.setGuest(true);

        if(guestRide.getRoute() != null) {
            Location start = guestRide.getRoute().getOrigin();
            Location end = guestRide.getRoute().getDestination();
            dto.setOrigin(new LocationDTO(start.getLongitude(), start.getLatitude(), start.getAddress()));
            dto.setDestination(new LocationDTO(end.getLongitude(), end.getLatitude(), end.getAddress()));
        }

        return dto;
    }

    private static boolean isHistorical(RideStatus status) {
        return status == RideStatus.FINISHED || status == RideStatus.STOPPED
                || status == RideStatus.CANCELED || status == RideStatus.FAILED;
    }

    private static LocalDateTime historyDate(LocalDateTime start, LocalDateTime created) {
        return start != null ? start : created;
    }

    private static LocalDateTime dtoHistoryDate(DriverRideHistoryResponseDTO dto) {
        return historyDate(dto.getStartTime(), dto.getCreatedAt());
    }

    private static boolean isWithinDateRange(LocalDateTime value, LocalDate from, LocalDate to) {
        if (value == null) return from == null && to == null;
        LocalDate date = value.toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    @Override
    public DriverRegistrationResponseDTO registerDriver(
            DriverRegistrationRequestDTO request,
            String platform,
            String mobileRegistrationBaseUrl) {

        // Validation
        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        if (vehicleRepository.existsByRegistration(request.getVehicle().getRegistration())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vehicle registration already exists");
        }

        // Create Vehicle object
        Vehicle vehicle = new Vehicle();
        vehicle.setModel(request.getVehicle().getModel());
        vehicle.setType(request.getVehicle().getType());
        vehicle.setRegistration(request.getVehicle().getRegistration());
        vehicle.setSeats(request.getVehicle().getSeats());
        vehicle.setIsBabyFriendly(request.getVehicle().isBabyFriendly());
        vehicle.setIsPetFriendly(request.getVehicle().isPetFriendly());
        vehicleRepository.save(vehicle);

        // Create Driver object
        Driver driver = new Driver();
        driver.setName(request.getName());
        driver.setSurname(request.getSurname());
        driver.setEmail(request.getEmail());
        // Temporary password until driver changes it
        driver.setPassword(UUID.randomUUID().toString());
        driver.setGender(request.getGender());
        driver.setAddress(request.getAddress());
        driver.setPhone(request.getPhone());
        driver.setStatus(DriverStatus.PENDING);
        driver.setVehicle(vehicle); // set vehicle
        // Random token to keep track which driver is being registered
        driver.setRegistrationToken(UUID.randomUUID().toString());
        driver.setRegistrationTokenExpiry(LocalDateTime.now().plusHours(24));

        // Write in database
        Driver saved = driverRepository.save(driver);

        // Determine registration link based on platform
        String registrationLink;
        if (platform.equalsIgnoreCase("mobile")) {
            // Email clients commonly remove custom schemes such as clickanddrive://.
            // Use a regular HTTP link and let the backend redirect it to the app.
            registrationLink = UriComponentsBuilder.fromUriString(mobileRegistrationBaseUrl)
                    .queryParam("token", driver.getRegistrationToken())
                    .build()
                    .encode()
                    .toUriString();
        } else {
            // Web app by default
            registrationLink = "http://localhost:4200/complete-registration?token=" + driver.getRegistrationToken();
        }

        // Sending driver email to set up password
        emailService.sendDriverRegistrationEmail(driver, registrationLink);

        // Map object to response
        return new DriverRegistrationResponseDTO(
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                saved.getSurname(),
                saved.getStatus()
        );
    }

    @Override
    public void completeRegistration(CompleteRegistrationRequestDTO request) {
        // Validation
        // Are password and confirmPassword the same
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            try {
                throw new BadRequestException("Password and confirm password are not the same");
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }

        // Find driver by registration token
        try {
            Driver driver = driverRepository.findByRegistrationToken(request.getToken()).orElseThrow(() -> new BadRequestException("Invalid or expired token"));

            if (driver.getRegistrationTokenExpiry() == null
                    || driver.getRegistrationTokenExpiry().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Invalid or expired token");
            }

            // Validation
            // Check if password is already set
            if (driver.getRegistrationToken() == null) {
                throw new BadRequestException("You are already registered");
            }

            // Set password
            driver.setPassword(passwordEncoder.encode(request.getPassword()));

            // Delete registration token
            driver.setRegistrationToken(null);
            driver.setRegistrationTokenExpiry(null);

            driver.setStatus(DriverStatus.ACTIVE);
            driver.setWorkMinutes(0);

            driverRepository.save(driver);

            // Send email to confirm successful registration
            EmailDetails confirmEmail = new EmailDetails();
            confirmEmail.setRecipient(driver.getEmail());
            confirmEmail.setSubject("Registration completed");
            confirmEmail.setMsgBody(
                    "Hello " + driver.getName() + ",\n\n" +
                            "Your registration is now complete! You can log in to ClickAndDrive.\n\n" +
                            "Best regards,\nClickAndDrive Team"
            );
            emailService.sendsSimpleMail(confirmEmail);

        } catch (BadRequestException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        return driverRepository.findByRegistrationToken(token).isPresent();
    }

    // Long id is drivers id
    @Override
    public VehicleResponseDTO getDriverVehicle(Long id) {
        // First find the driver
        Driver driver = driverRepository.findById(id).orElseThrow(() -> new RuntimeException("No driver with id exists"));

        // Fetch his vehicle
        Vehicle vehicle = driver.getVehicle();

        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getModel(),
                vehicle.getType(),
                vehicle.getRegistration(),
                vehicle.getSeats(),
                vehicle.getIsBabyFriendly(),
                vehicle.getIsPetFriendly()
        );
    }

    public boolean isOwnerOrAdmin(Long idFromPath) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();

        if (principal instanceof User) {
            User loggedInUser = (User) principal;

            // Debug
//            System.out.println("Ulogovan korisnik ID: " + loggedInUser.getId());
//            System.out.println("ID iz URL-a: " + idFromPath);

            // Admin?
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
            if (isAdmin) return true;

            // Owner?
            return loggedInUser.getId().equals(idFromPath);
        }

        return false;
    }

    @Override
    public List<UserProfileResponseDTO> getAllDrivers() {
        List<Driver> drivers = driverRepository.findAll();

        // Convert object to responseDTO
        return drivers.stream().map(this::mapDriverToDTO).collect(Collectors.toList());
    }

    // Helper
    private UserProfileResponseDTO mapDriverToDTO(Driver driver) {
        return new UserProfileResponseDTO(
                driver.getId(),
                driver.getEmail(),
                driver.getName(),
                driver.getSurname(),
                driver.getAddress(),
                driver.getPhone(),
                driver.getProfileImageUrl(),
                driver.isBlocked(),
                driver.getBlockReason()
        );
    }
}
