package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.*;
import com.example.backendspringboot.dto.response.*;
import com.example.backendspringboot.model.*;

import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.UserRepository;
import com.example.backendspringboot.services.interfaces.GuestRideService;
import com.example.backendspringboot.services.interfaces.ReviewService;
import com.example.backendspringboot.services.interfaces.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rides")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class RideController {

    // Service
    private final RideService rideService;
    private final ReviewService reviewService;
    private final GuestRideService guestRideService;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/create-ride")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RideResponseDTO> createRide(
            @Valid @RequestBody CreateRideRequestDTO request,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof Passenger passenger)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        request.setPassengerId(passenger.getId());

        RideResponseDTO response = rideService.createRide(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Ordering ride from favorites
    @PostMapping("/favorites")
    public ResponseEntity<RouteFromFavoritesResponseDTO> createRideFromFavorites(
            @RequestBody RouteFromFavoritesRequestDTO request) {
        LocationDTO origin = new LocationDTO(
                45.2671,
                19.8335,
                "Novi Sad"
        );

        LocationDTO destination = new LocationDTO(
                44.7866,
                20.4489,
                "Beograd"
        );

        RouteFromFavoritesResponseDTO response =
                new RouteFromFavoritesResponseDTO(
                        1L,
                        origin,
                        destination,
                        100,
                        90,
                        5
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Starting the ride
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> startRide(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> body,
            Authentication authentication) {

        Driver driver = (Driver) authentication.getPrincipal();
        boolean isGuest = body != null && body.getOrDefault("isGuest", false);
        rideService.startRide(id, isGuest, driver.getEmail());

        // WebSocket msg
        Map<String, Object> message = Map.of(
                "type", "RIDE_STARTED",
                "rideId", id,
                "isGuest", isGuest
        );
        messagingTemplate.convertAndSend("/topic/ride-events", message);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/ping")
    public String ping() {
            return "RideController radi!";
    }

    @PostMapping("/cancel/{id}")
    public ResponseEntity<Void> cancelRide(
            @PathVariable Long id,
            @RequestBody RideCancellationRequestDTO request
    ) {
        rideService.cancelAnyRide(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stopRide(
            @PathVariable Long id,
            @Valid @RequestBody RideStopRequestDTO request
    ) {
        rideService.stopRide(id, request);
        return ResponseEntity.ok().build();
    }

    // 2.6.2: Following the ride
    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasAnyRole('USER', 'DRIVER', 'ADMIN')")
    public ResponseEntity<RideTrackingResponseDTO> getRideTracking(
            @PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User requester)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        rideService.assertCanTrackRide(id, requester);
        return ResponseEntity.ok(rideService.getRideTracking(id));
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Page<ScheduledRideResponseDTO>> getDriverScheduledRides(
            @PathVariable Long driverId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            Authentication authentication
    ) {
        Driver driver = (Driver) authentication.getPrincipal();
        if (!driver.getId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Nemate pristup vožnjama drugog vozača.");
        }
        Page<ScheduledRideResponseDTO> rides = rideService.getDriverScheduledRides(driverId, page, size);
        return ResponseEntity.ok(rides);
    }

    // 2.6.2: inconsistency report
    @PostMapping("/{id}/inconsistency-report")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<InconsistencyReportResponseDTO> reportInconsistency(
            @PathVariable Long id,
            @Valid @RequestBody InconsistencyReportRequestDTO dto,
            Authentication authentication) {
        Passenger passenger = (Passenger) authentication.getPrincipal();
        String passengerEmail = passenger.getEmail();
        System.out.println("passenger email: " + passengerEmail);

        return ResponseEntity.ok(rideService.reportInconsistency(id, dto, passengerEmail));
    }

    // 2.7: Finish ride
    @PutMapping("/{id}/finish")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> finishRide(@PathVariable Long id,
                                           @RequestParam(name = "distanceKm", required = false)
                                                Double distance,
                                           @RequestParam boolean isGuest,
                                           Authentication authentication) {
        Driver driver = (Driver) authentication.getPrincipal();
        String driverEmail = driver.getEmail();
        if(distance == null) {
            distance = 0.0;
        }

        rideService.finishRide(id, driverEmail, distance, isGuest);

        Map<String, Object> message = Map.of(
                "type", "RIDE_FINISHED",
                "rideId", id,
                "isGuest", isGuest
        );
        messagingTemplate.convertAndSend("/topic/ride-events", message);

        return ResponseEntity.ok().build();
    }

    // 2.8: Rating
    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createReview(@PathVariable Long id, @Valid @RequestBody ReviewRequestDTO dto) {
        dto.setRideId(id);
        reviewService.createReview(dto);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/{id}/panic")
    public ResponseEntity<Void> panicRide(@PathVariable Long id) {
        rideService.panic(id);
        return ResponseEntity.ok().build();
    }
}
