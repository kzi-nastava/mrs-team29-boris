package com.example.backendspringboot.controller;

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
import org.springframework.security.core.Authentication;
import com.example.backendspringboot.model.Passenger;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<Page<RouteFromFavoritesResponseDTO>> getFavoriteRoutes(
            @PathVariable Long passengerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {
        assertOwnPassenger(passengerId, authentication);
        Page<RouteFromFavoritesResponseDTO> favoriteRoutes = passengerService.getFavoriteRoutesForPassenger(passengerId, PageRequest.of(page, size));
        return ResponseEntity.ok(favoriteRoutes);
    }

    @GetMapping("/me/favorite-routes")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<RouteFromFavoritesResponseDTO>> getOwnFavoriteRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Authentication authentication) {
        Passenger passenger = authenticatedPassenger(authentication);
        return ResponseEntity.ok(passengerService.getFavoriteRoutesForPassenger(
                passenger.getId(), PageRequest.of(page, size)));
    }

    @PostMapping("/me/favorite-routes/rides/{rideId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RouteFromFavoritesResponseDTO> addOwnFavoriteRoute(
            @PathVariable Long rideId, Authentication authentication) {
        Passenger passenger = authenticatedPassenger(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                passengerService.addRideRouteToFavorites(passenger.getId(), rideId));
    }

    @DeleteMapping("/me/favorite-routes/{routeId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removeOwnFavoriteRoute(
            @PathVariable Long routeId, Authentication authentication) {
        Passenger passenger = authenticatedPassenger(authentication);
        passengerService.removeFromFavoriteRoutes(passenger.getId(), routeId);
        return ResponseEntity.noContent().build();
    }

    private Passenger authenticatedPassenger(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof Passenger passenger)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return passenger;
    }

    private void assertOwnPassenger(Long passengerId, Authentication authentication) {
        Passenger passenger = authenticatedPassenger(authentication);
        if (!passenger.getId().equals(passengerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Nemate pristup omiljenim rutama drugog putnika.");
        }
    }

    // Remove a route from favorites
    @DeleteMapping("/{passengerId}/{routeId}/remove-route")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removeFromFavorites(
            @PathVariable Long passengerId, @PathVariable Long routeId,
            Authentication authentication) {
        assertOwnPassenger(passengerId, authentication);
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
