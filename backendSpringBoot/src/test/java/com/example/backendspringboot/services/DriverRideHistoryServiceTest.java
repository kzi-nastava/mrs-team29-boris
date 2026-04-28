package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.response.DriverRideHistoryResponseDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverRideHistoryServiceTest {
    @Mock DriverRepository driverRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock EmailServiceImpl emailService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RideRepository rideRepository;
    @Mock UserRepository userRepository;
    @Mock GuestRideRepository guestRideRepository;
    @InjectMocks DriverServiceImpl service;

    private Driver driver;

    @BeforeEach
    void authenticateDriver() {
        driver = new Driver();
        driver.setId(7L);
        driver.setEmail("driver@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(driver, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void historyFiltersInclusiveDatesExcludesActiveRidesAndSortsNewestFirst() {
        Ride old = ride(1L, RideStatus.FINISHED, LocalDateTime.of(2026, 1, 10, 9, 0));
        Ride recent = ride(2L, RideStatus.CANCELED, LocalDateTime.of(2026, 1, 20, 9, 0));
        Ride outside = ride(3L, RideStatus.FINISHED, LocalDateTime.of(2025, 12, 31, 9, 0));
        Ride active = ride(4L, RideStatus.STARTED, LocalDateTime.of(2026, 1, 25, 9, 0));
        when(rideRepository.findAllByDriverId(7L))
                .thenReturn(List.of(old, recent, outside, active));
        when(guestRideRepository.findAllByDriverId(7L)).thenReturn(Collections.emptyList());

        List<DriverRideHistoryResponseDTO> result = service.getDriverRideHistory(
                7L, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));

        assertEquals(List.of(2L, 1L), result.stream().map(
                DriverRideHistoryResponseDTO::getId).toList());
        assertTrue(result.get(0).isCanceled());
    }

    @Test
    void detailReturnsAllDriverSpecificData() {
        Passenger passenger = new Passenger();
        passenger.setId(11L);
        passenger.setName("Ana");
        passenger.setSurname("Anić");
        passenger.setEmail("ana@example.com");
        passenger.setPhone("060123456");

        Ride ride = ride(9L, RideStatus.CANCELED, LocalDateTime.of(2026, 2, 1, 12, 0));
        ride.setPassengers(List.of(passenger));
        ride.setCancelledBy(passenger);
        ride.setCancellationReason("Putnik je odustao");
        ride.setPanicPressed(true);
        ride.setPrice(1234.5);
        when(rideRepository.findById(9L)).thenReturn(Optional.of(ride));

        DriverRideHistoryResponseDTO result = service.getDriverRideHistoryDetail(7L, 9L, false);

        assertEquals("ana@example.com", result.getPassengers().get(0).getEmail());
        assertEquals("ana@example.com", result.getCanceledBy());
        assertEquals("Putnik je odustao", result.getCancellationReason());
        assertEquals(1234.5, result.getTotalPrice());
        assertTrue(result.isPanicPressed());
    }

    @Test
    void detailRejectsRideBelongingToAnotherDriver() {
        Driver other = new Driver();
        other.setId(99L);
        Ride ride = ride(10L, RideStatus.FINISHED, LocalDateTime.now());
        ride.setDriver(other);
        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getDriverRideHistoryDetail(7L, 10L, false));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private Ride ride(long id, RideStatus status, LocalDateTime start) {
        Location origin = new Location();
        origin.setAddress("Bulevar oslobođenja, Novi Sad");
        origin.setLatitude(45.25);
        origin.setLongitude(19.84);
        Location destination = new Location();
        destination.setAddress("Futoški put, Novi Sad");
        destination.setLatitude(45.24);
        destination.setLongitude(19.80);
        Route route = new Route();
        route.setOrigin(origin);
        route.setDestination(destination);

        Ride ride = new Ride();
        ride.setId(id);
        ride.setDriver(driver);
        ride.setStatus(status);
        ride.setStartTime(start);
        ride.setEndTime(start.plusMinutes(15));
        ride.setRoute(route);
        ride.setPassengers(Collections.emptyList());
        ride.setReviews(Collections.emptyList());
        ride.setInconsistencyReports(Collections.emptyList());
        return ride;
    }
}
