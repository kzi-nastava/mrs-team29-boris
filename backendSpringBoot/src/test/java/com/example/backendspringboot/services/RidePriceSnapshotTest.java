package com.example.backendspringboot.services;

import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import com.example.backendspringboot.services.interfaces.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RidePriceSnapshotTest {
    @Mock RideRepository rideRepository;
    @Mock GuestRideRepository guestRideRepository;
    @Mock LocationRepository locationRepository;
    @Mock RouteRepository routeRepository;
    @Mock DriverRepository driverRepository;
    @Mock UserRepository userRepository;
    @Mock PanicRepository panicRepository;
    @Mock PassengerRepository passengerRepository;
    @Mock InconsistencyReportRepository inconsistencyReportRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock EmailService emailService;
    @Mock VehiclePriceRepository vehiclePriceRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock AppNotificationService notificationService;
    @InjectMocks RideServiceImpl service;

    @Test
    void finishingRideUsesBookingSnapshotInsteadOfCurrentPriceList() {
        Location origin = new Location(1L, 19.8, 45.2, "A");
        Location destination = new Location(2L, 19.9, 45.3, "B");
        Route route = new Route();
        route.setOrigin(origin);
        route.setDestination(destination);

        Vehicle vehicle = new Vehicle();
        vehicle.setType(VehicleType.STANDARD);
        Driver driver = new Driver();
        driver.setVehicle(vehicle);
        Passenger creator = new Passenger();
        creator.setId(10L);
        creator.setEmail("passenger@example.com");
        Passenger linked = new Passenger();
        linked.setId(11L);
        linked.setEmail("linked@example.com");

        Ride ride = new Ride();
        ride.setId(5L);
        ride.setStatus(RideStatus.STARTED);
        ride.setStartTime(LocalDateTime.now().minusMinutes(10));
        ride.setRoute(route);
        ride.setDriver(driver);
        ride.setRideCreator(creator);
        ride.setPassengers(List.of(linked));
        ride.setLinkedPassengerEmails(List.of("linked@example.com", "external@example.com"));
        ride.setVehicleTypeAtBooking(VehicleType.STANDARD);
        ride.setBasePriceAtBooking(200);
        ride.setPricePerKmAtBooking(100);
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride));

        service.finishRide(5L, "driver@example.com", 5, false);

        assertEquals(700, ride.getPrice());
        assertEquals(5, ride.getDistanceKm());
        verify(vehiclePriceRepository, never()).findTopBy();
        verify(emailService, times(3)).sendRideTrackingEmail(anyString(), anyString(),
                anyString(), eq("clickanddrive://ride-tracking?rideId=5"));
        verify(notificationService).notify(eq(linked), eq(ride), eq("RIDE_FINISHED"),
                anyString(), eq("ride:5:finished:11"));
        verify(notificationService).notify(eq(creator), eq(ride), eq("RIDE_FINISHED"),
                anyString(), eq("ride:5:finished:10"));
    }
}
