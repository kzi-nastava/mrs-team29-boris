package com.example.demo.controller;

import com.example.backendspringboot.dto.response.RouteFromFavoritesResponseDTO;
import com.example.backendspringboot.services.PassengerServiceImpl;
import com.example.backendspringboot.services.RideServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/passenger")
public class PassengerController {

    private final PassengerServiceImpl passengerService;
    private final RideServiceImpl rideService;

    // Getting passengers list of favorite routes to display
    @GetMapping("/{passengerId}/favorite-routes")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<RouteFromFavoritesResponseDTO>> getFavoriteRoutes(@PathVariable Long passengerId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "3") int size) {
        Page<RouteFromFavoritesResponseDTO> favoriteRoutes = passengerService.getFavoriteRoutesForPassenger(passengerId, PageRequest.of(page, size));
        return ResponseEntity.ok(favoriteRoutes);
    }

    // Remove a route from favorites
    @DeleteMapping("/{passengerId}/{routeId}/remove-route")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removeFromFavorites(@PathVariable Long passengerId, @PathVariable Long routeId) {
        passengerService.removeFromFavoriteRoutes(passengerId, routeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/ride-history")
    public ResponseEntity<?> getPassengerHistory(@PathVariable Long id) {

        return ResponseEntity.ok(passengerService.getPassengerRideHistory(id));
    }

    @GetMapping("/rides/{rideId}/details")
    public ResponseEntity<?> getRideDetails(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.getRideDetails(rideId));
    }
}
