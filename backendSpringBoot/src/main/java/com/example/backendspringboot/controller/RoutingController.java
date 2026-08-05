package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.RoutePreviewRequestDTO;
import com.example.backendspringboot.dto.response.RoutePreviewResponseDTO;
import com.example.backendspringboot.services.RoutingResult;
import com.example.backendspringboot.services.RoutingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
public class RoutingController {
    private final RoutingService routingService;

    @PostMapping("/preview")
    @PreAuthorize("hasRole('USER')")
    public RoutePreviewResponseDTO preview(@Valid @RequestBody RoutePreviewRequestDTO request) {
        RoutingResult result = routingService.calculate(
                request.getOrigin(), request.getStops(), request.getDestination());
        return new RoutePreviewResponseDTO(result.distanceKm(), result.durationMinutes(),
                result.geometry().stream()
                        .map(point -> new LocationDTO(point.getLongitude(), point.getLatitude(), null))
                        .toList());
    }
}
