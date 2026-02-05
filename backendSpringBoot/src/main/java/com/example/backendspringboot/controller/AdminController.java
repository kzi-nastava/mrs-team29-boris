package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.NoteRequestDTO;
import com.example.backendspringboot.dto.request.DriverRegistrationRequestDTO;
import com.example.backendspringboot.dto.response.*;
import com.example.backendspringboot.dto.VehiclePriceDTO;

import com.example.backendspringboot.model.Review;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.model.VehiclePrice;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.VehiclePriceRepository;
import com.example.backendspringboot.services.interfaces.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AdminController {

    // Services
    private final AdminService adminService;
    private final DriverService driverService;
    private final PassengerService passengerService;
    private final UserService userService;
    private final PanicService panicService;
    private final VehiclePriceRepository priceRepository;
    private final RideRepository rideRepository;
    private final VehiclePriceService priceService;

    @PostMapping("/drivers")
    public ResponseEntity<DriverRegistrationResponseDTO> registerDriver(
            @Valid @RequestBody DriverRegistrationRequestDTO request,
            @RequestParam(defaultValue = "web") String platform) {

        // Call service function to register a new driver into the app
        DriverRegistrationResponseDTO response = driverService.registerDriver(request, platform);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rides")
    public ResponseEntity<List<RideHistoryResponseDTO>> getRideHistory(
            @RequestParam Long userId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        return ResponseEntity.ok(new ArrayList<>());
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<RideDetailsResponseDTO> getRideDetails(@PathVariable Long id) {
        Ride ride = rideRepository.findById(id).orElseThrow();

        RideDetailsResponseDTO dto = new RideDetailsResponseDTO();
        dto.setRideId(ride.getId());

        if (ride.getRoute() != null) {
            if (ride.getRoute().getOrigin() != null) {
                dto.setStartAddress(ride.getRoute().getOrigin().getAddress());
                dto.setOrigin(new LocationDTO(
                        ride.getRoute().getOrigin().getLongitude(),
                        ride.getRoute().getOrigin().getLatitude(),
                        ride.getRoute().getOrigin().getAddress()
                ));
            }

            if (ride.getRoute().getDestination() != null) {
                dto.setEndAddress(ride.getRoute().getDestination().getAddress());
                dto.setDestination(new LocationDTO(
                        ride.getRoute().getDestination().getLongitude(),
                        ride.getRoute().getDestination().getLatitude(),
                        ride.getRoute().getDestination().getAddress()
                ));
            }
        }

        dto.setStartTime(ride.getStartTime());
        dto.setEndTime(ride.getEndTime());
        dto.setCanceled(ride.getCancelledBy() != null);
        dto.setCanceledBy(ride.getCancelledBy() != null ? ride.getCancelledBy().getEmail() : null);
        dto.setPrice(ride.getPrice());
        dto.setPanicTriggered(ride.isPanicPressed());
        dto.setStatus(ride.getStatus() != null ? ride.getStatus().toString() : null);

        if (ride.getDriver() != null) {
            dto.setDriverId(ride.getDriver().getId());
            dto.setDriverName(ride.getDriver().getName() + " " + ride.getDriver().getSurname());
        }

        if (ride.getPassengers() != null) {
            List<UserProfileResponseDTO> passengerDtos = ride.getPassengers().stream()
                    .map(p -> new UserProfileResponseDTO(
                            p.getId(),
                            p.getEmail(),
                            p.getName(),
                            p.getSurname(),
                            p.getAddress(),
                            p.getPhone(),
                            p.getProfileImageUrl(),
                            p.isBlocked(),
                            p.getBlockReason()
                    ))
                    .toList();
            dto.setPassengers(passengerDtos);
        }

        if (ride.getReviews() != null && !ride.getReviews().isEmpty()) {
            Review review = ride.getReviews().get(0);
            dto.setRating(review.getDriverRating());
            dto.setComment(review.getComment());
        }

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/rides/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminRideStateResponseDTO>> getAllActiveRides() {
        List<Ride> activeRides = rideRepository.findAllByStatus(RideStatus.STARTED);

        List<AdminRideStateResponseDTO> response = activeRides.stream().map(ride -> {
            // Safe check for location
            LocationDTO currentLoc = null;
            if (ride.getCurrentLocation() != null) {
                currentLoc = new LocationDTO(
                        ride.getCurrentLocation().getLatitude(),
                        ride.getCurrentLocation().getLongitude(),
                        ride.getCurrentLocation().getAddress()
                );
            } else {
                // if null, then it's ended
                currentLoc = new LocationDTO(ride.getRoute().getDestination().getLongitude(),
                        ride.getRoute().getDestination().getLatitude(),
                        ride.getRoute().getDestination().getAddress());
            }

            // Safe check for route
            String origin = (ride.getRoute() != null && ride.getRoute().getOrigin() != null)
                    ? ride.getRoute().getOrigin().getAddress() : "Rumenacka 100";
            String dest = (ride.getRoute() != null && ride.getRoute().getDestination() != null)
                    ? ride.getRoute().getDestination().getAddress() : "Rumenacka 140";

            return new AdminRideStateResponseDTO(
                    ride.getId(),
                    ride.getDriver().getEmail(),
                    ride.getPassengers().stream().map(p -> p.getEmail()).toList(),
                    currentLoc,
                    origin,
                    dest,
                    ride.getStartTime(),
                    ride.getStatus().toString()
            );
        }).toList();

        return ResponseEntity.ok(response);
    }

    // 2.14: GET prices
    @GetMapping("/prices")
    public ResponseEntity<VehiclePriceDTO> getVehiclePrices() {
        return ResponseEntity.ok(priceService.getPrices());
    }

    // 2.14: UPDATE prices
    @PutMapping("/prices")
    public ResponseEntity<VehiclePriceDTO> updateVehiclePrices(@RequestBody VehiclePriceDTO request) {
        VehiclePriceDTO updated = priceService.updatePrices(request);
        return ResponseEntity.ok(updated);
    }

    // GET all drivers
    @GetMapping("/drivers/all")
    public ResponseEntity<List<UserProfileResponseDTO>> getAllDrivers() {
        List<UserProfileResponseDTO> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(drivers);
    }

    // GET all passengers
    @GetMapping("/passengers/all")
    public ResponseEntity<List<UserProfileResponseDTO>> getAllPassengers() {
        List<UserProfileResponseDTO> passengers = passengerService.getAllPassengers();
        return ResponseEntity.ok(passengers);
    }

    @GetMapping("/drivers/all/paged")
    public ResponseEntity<Page<UserProfileResponseDTO>> getAllDriversPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {
        return ResponseEntity.ok(adminService.getAllDriversPaged(PageRequest.of(page, size)));
    }

    @GetMapping("/passengers/all/paged")
    public ResponseEntity<Page<UserProfileResponseDTO>> getAllPassengersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {
        return ResponseEntity.ok(adminService.getAllPassengersPaged(PageRequest.of(page, size)));
    }

    // Block a user (driver or passenger)
    @PutMapping("/users/{id}/block")
    public ResponseEntity<UserProfileResponseDTO> blockUser(@PathVariable Long id, @RequestBody(required = false) NoteRequestDTO request) {
        String reason = (request != null) ? request.getMessage() : null;
        UserProfileResponseDTO updatedUser = userService.blockUser(id, reason);

        return ResponseEntity.ok(updatedUser);
    }

    // Unblock a user
    @PutMapping("/users/{id}/unblock")
    public ResponseEntity<UserProfileResponseDTO> unblockUser(@PathVariable Long id) {
        UserProfileResponseDTO updatedUser = userService.unblockUser(id);
        return ResponseEntity.ok(updatedUser);
    }

    // Leave a note
    @PutMapping("/users/{id}/note")
    public ResponseEntity<UserProfileResponseDTO> leaveNote(@PathVariable Long id, @RequestBody NoteRequestDTO request) {
        UserProfileResponseDTO updatedUser = userService.setNote(id, request.getMessage());
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/ride-history")
    public ResponseEntity<List<AdminRideHistoryResponseDTO>> getRideHistory(
            @RequestParam Long id,
            @RequestParam String role,
            @RequestParam(defaultValue = "date") String sortBy) {

        // map "date" to startTime
        if ("date".equalsIgnoreCase(sortBy)) {
            sortBy = "startTime";
        }

        List<AdminRideHistoryResponseDTO> rides = adminService.getRideHistory(id, role, sortBy);
        return ResponseEntity.ok(rides);
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllNonAdminUsers());
    }
}
