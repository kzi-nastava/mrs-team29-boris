package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.ReportRequestDTO;
import com.example.backendspringboot.dto.response.ReportResponseDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.GuestRideRepository;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock RideRepository rideRepository;
    @Mock UserRepository userRepository;
    @Mock GuestRideRepository guestRideRepository;
    @InjectMocks ReportServiceImpl service;

    @Test
    void driverReportCombinesRegisteredAndGuestRides() {
        Driver driver = new Driver();
        driver.setId(7L);
        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(driver));
        when(rideRepository.findFinishedRidesByDriverAndDateRange(
                7L, day(1, 0, 0), day(2, 23, 59, 59)))
                .thenReturn(List.of(regularRide(day(1, 12, 0), 10, 1000)));
        when(guestRideRepository.findFinishedByDriverAndDateRange(
                7L, day(1, 0, 0), day(2, 23, 59, 59)))
                .thenReturn(List.of(guestRide(day(2, 12, 0), 5, 600)));

        ReportResponseDTO result = service.generateReport(request(1, 2, null, null),
                "driver@example.com", "DRIVER");

        assertTrue(result.isEarnings());
        assertEquals(2, result.getSummary().getTotalRides());
        assertEquals(15, result.getSummary().getTotalKilometers());
        assertEquals(1600, result.getSummary().getTotalMoney());
        assertEquals(1, result.getSummary().getAvgRidesPerDay());
        assertEquals(2, result.getDailyStats().get(1).getCumulativeRides());
    }

    @Test
    void passengerSpendingIsReportedAsPositiveAmount() {
        Passenger passenger = new Passenger();
        passenger.setId(11L);
        when(userRepository.findByEmail("passenger@example.com"))
                .thenReturn(Optional.of(passenger));
        when(rideRepository.findFinishedRidesByPassengerAndDateRange(
                11L, day(1, 0, 0), day(2, 23, 59, 59)))
                .thenReturn(List.of(regularRide(day(1, 15, 0), 8, 900)));

        ReportResponseDTO result = service.generateReport(request(1, 2, null, null),
                "passenger@example.com", "PASSENGER");

        assertFalse(result.isEarnings());
        assertEquals(900, result.getSummary().getTotalMoney());
        assertEquals(450, result.getSummary().getAvgMoneyPerDay());
    }

    @Test
    void adminAllDriversIncludesGuestRideEarnings() {
        when(rideRepository.findAllFinishedRidesByDriversAndDateRange(
                day(1, 0, 0), day(1, 23, 59, 59)))
                .thenReturn(Collections.emptyList());
        when(guestRideRepository.findAllFinishedByDriversAndDateRange(
                day(1, 0, 0), day(1, 23, 59, 59)))
                .thenReturn(List.of(guestRide(day(1, 12, 0), 4, 500)));

        ReportResponseDTO result = service.generateReport(
                request(1, 1, null, "ALL_DRIVERS"), "admin@example.com", "ADMIN");

        assertTrue(result.isEarnings());
        assertEquals(1, result.getSummary().getTotalRides());
        assertEquals(500, result.getSummary().getTotalMoney());
    }

    @Test
    void rejectsMissingOrReversedDateRange() {
        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> service.generateReport(new ReportRequestDTO(), "x", "DRIVER"));
        assertEquals(HttpStatus.BAD_REQUEST, missing.getStatusCode());

        ResponseStatusException reversed = assertThrows(ResponseStatusException.class,
                () -> service.generateReport(request(2, 1, null, null), "x", "DRIVER"));
        assertEquals(HttpStatus.BAD_REQUEST, reversed.getStatusCode());
    }

    private static ReportRequestDTO request(int fromDay, int toDay, Long id, String type) {
        return new ReportRequestDTO(day(fromDay, 10, 0), day(toDay, 10, 0), id, type);
    }

    private static Ride regularRide(LocalDateTime end, double distance, double price) {
        Route route = new Route();
        route.setDistance(distance);
        Ride ride = new Ride();
        ride.setEndTime(end);
        ride.setRoute(route);
        ride.setPrice(price);
        return ride;
    }

    private static GuestRide guestRide(LocalDateTime end, double distance, double price) {
        Route route = new Route();
        route.setDistance(distance);
        GuestRide ride = new GuestRide();
        ride.setEndTime(end);
        ride.setRoute(route);
        ride.setPrice(price);
        return ride;
    }

    private static LocalDateTime day(int day, int hour, int minute) {
        return day(day, hour, minute, 0);
    }

    private static LocalDateTime day(int day, int hour, int minute, int second) {
        return LocalDateTime.of(2026, 8, day, hour, minute, second);
    }
}
