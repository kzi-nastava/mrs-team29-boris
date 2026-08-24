package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.response.RouteFromFavoritesResponseDTO;
import com.example.backendspringboot.model.Location;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.model.Route;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerFavoriteRouteServiceTest {
    @Mock PassengerRepository passengerRepository;
    @Mock RouteRepository routeRepository;
    @Mock RideRepository rideRepository;
    @InjectMocks PassengerServiceImpl passengerService;

    private Passenger passenger;
    private Ride ride;
    private Route route;

    @BeforeEach
    void setup() {
        passenger = new Passenger();
        passenger.setId(10L);
        passenger.setFavoriteRoutes(new ArrayList<>());

        route = new Route();
        route.setId(30L);
        route.setOrigin(location("Polazište", 19.8, 45.2));
        route.setDestination(location("Odredište", 19.9, 45.3));
        route.setStops(new ArrayList<>(List.of(
                location("Stanica", 19.85, 45.25))));

        ride = new Ride();
        ride.setId(20L);
        ride.setStatus(RideStatus.FINISHED);
        ride.setRideCreator(passenger);
        ride.setRoute(route);

        when(passengerRepository.findById(10L)).thenReturn(Optional.of(passenger));
        when(rideRepository.findById(20L)).thenReturn(Optional.of(ride));
    }

    @Test
    void participantCanAddFinishedRideRouteWithOrderedStops() {
        RouteFromFavoritesResponseDTO result = passengerService
                .addRideRouteToFavorites(10L, 20L);

        assertEquals(1, passenger.getFavoriteRoutes().size());
        assertSame(route, passenger.getFavoriteRoutes().get(0));
        assertEquals("Polazište", result.getOrigin().getAddress());
        assertEquals("Stanica", result.getStops().get(0).getAddress());
        verify(passengerRepository).save(passenger);
    }

    @Test
    void sameRouteIsNotAddedTwice() {
        passenger.getFavoriteRoutes().add(route);

        passengerService.addRideRouteToFavorites(10L, 20L);

        assertEquals(1, passenger.getFavoriteRoutes().size());
        verify(passengerRepository, never()).save(passenger);
    }

    @Test
    void routeCannotBeFavoritedBeforeRideFinishes() {
        ride.setStatus(RideStatus.STARTED);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passengerService.addRideRouteToFavorites(10L, 20L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Ruta se može sačuvati tek nakon završetka vožnje.",
                exception.getReason());
    }

    private static Location location(String address, double longitude, double latitude) {
        Location location = new Location();
        location.setAddress(address);
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        return location;
    }
}
