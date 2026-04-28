package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.request.CompleteRegistrationRequestDTO;
import com.example.backendspringboot.dto.response.DriverRideHistoryResponseDTO;
import com.example.backendspringboot.dto.response.VehicleResponseDTO;
import com.example.backendspringboot.services.interfaces.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class DriverController {

    // Service
    private final DriverService driverService;

    // 2.9.2: Driver history
    @GetMapping("/{id}/ride-history")
    public ResponseEntity<?> getDriverHistory(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        // get logged in Email
        if (!driverService.isOwnerOrAdmin(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot see others driver histories!");
        }

        return ResponseEntity.ok(driverService.getDriverRideHistory(id, from, to));
    }

    @GetMapping("/{id}/ride-history/{rideId}")
    public ResponseEntity<DriverRideHistoryResponseDTO> getDriverHistoryDetail(
            @PathVariable Long id,
            @PathVariable Long rideId,
            @RequestParam(defaultValue = "false") boolean guest) {
        if (!driverService.isOwnerOrAdmin(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(driverService.getDriverRideHistoryDetail(id, rideId, guest));
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<Map<String, String>> completeRegistration(@Valid @RequestBody CompleteRegistrationRequestDTO request) {

        driverService.completeRegistration(request);
        return ResponseEntity.ok(Collections.singletonMap("message", "Completed registration"));
    }

    @GetMapping("/open-registration")
    public ResponseEntity<Void> openRegistrationInMobileApp(@RequestParam String token) {
        if (!driverService.isTokenValid(token)) {
            return ResponseEntity.notFound().build();
        }

        URI appUri = UriComponentsBuilder
                .fromUriString("clickanddrive://complete-registration")
                .queryParam("token", token)
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(appUri).build();
    }

    @GetMapping("/{id}/vehicle")
    public ResponseEntity<VehicleResponseDTO> getDriverVehicle(@PathVariable Long id) {
        VehicleResponseDTO response = driverService.getDriverVehicle(id);
        return ResponseEntity.ok(response);
    }
}
