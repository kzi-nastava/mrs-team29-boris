package com.example.backendspringboot.services;

import com.example.backendspringboot.model.Administrator;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.repositories.RideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideTrackingAuthorizationTest {
    @Mock RideRepository rideRepository;
    @InjectMocks RideServiceImpl service;

    @Test
    void onlyParticipantsDriverAndAdministratorCanTrackRide() {
        Passenger creator = passenger(1L);
        Passenger linked = passenger(2L);
        Passenger unrelated = passenger(3L);
        Driver driver = new Driver();
        driver.setId(4L);
        Administrator administrator = new Administrator();
        administrator.setId(5L);

        Ride ride = new Ride();
        ride.setRideCreator(creator);
        ride.setPassengers(List.of(linked));
        ride.setDriver(driver);
        when(rideRepository.findById(9L)).thenReturn(Optional.of(ride));

        assertDoesNotThrow(() -> service.assertCanTrackRide(9L, creator));
        assertDoesNotThrow(() -> service.assertCanTrackRide(9L, linked));
        assertDoesNotThrow(() -> service.assertCanTrackRide(9L, driver));
        assertDoesNotThrow(() -> service.assertCanTrackRide(9L, administrator));
        assertThrows(ResponseStatusException.class,
                () -> service.assertCanTrackRide(9L, unrelated));
    }

    private Passenger passenger(long id) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        return passenger;
    }
}
