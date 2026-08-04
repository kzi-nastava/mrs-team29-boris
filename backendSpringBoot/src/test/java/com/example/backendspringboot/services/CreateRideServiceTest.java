package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.CreateRideRequestDTO;
import com.example.backendspringboot.dto.response.RideResponseDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import com.example.backendspringboot.services.interfaces.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

// Testing service for ordering a new ride by registered user
@ExtendWith(MockitoExtension.class)
public class CreateRideServiceTest {

    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock private RideRepository rideRepository;
    @Mock private GuestRideRepository guestRideRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private VehiclePriceRepository vehiclePriceRepository;
    @Mock private AppNotificationService notificationService;

    @InjectMocks
    private RideServiceImpl rideService;

    private CreateRideRequestDTO validRequest;
    private Passenger passenger;

    @BeforeEach
    void setUp() {
        LocationDTO origin = new LocationDTO();
        origin.setLatitude(45.0);
        origin.setLongitude(19.0);
        origin.setAddress("Ulica A");

        LocationDTO destination = new LocationDTO();
        destination.setLatitude(46.0);
        destination.setLongitude(20.0);
        destination.setAddress("Ulica B");

        validRequest = new CreateRideRequestDTO();
        validRequest.setOrigin(origin);
        validRequest.setDestination(destination);
        validRequest.setPassengerId(1L);
        validRequest.setScheduledTime(LocalDateTime.now().plusMinutes(10));
        validRequest.setDurationMinutes(30);
        validRequest.setDistanceKm(5.0);
        validRequest.setBabyFriendly(false);
        validRequest.setPetFriendly(false);
        validRequest.setVehicleType(VehicleType.STANDARD);

        passenger = new Passenger();
        passenger.setId(1L);
        passenger.setBlocked(false);

        lenient().when(vehiclePriceRepository.findTopBy()).thenReturn(Optional.of(
                new VehiclePrice(1L, 150, 500, 250, 120)));
    }

    // Validate origin and destination are not the same
    @Test
    void whenOriginEqualsDestination_thenThrowException() {
        LocationDTO same = new LocationDTO();
        same.setLatitude(45.0);
        same.setLongitude(19.0);
        same.setAddress("Ista ulica");

        validRequest.setOrigin(same);
        validRequest.setDestination(same);

        assertThrows(RuntimeException.class, () -> rideService.createRide(validRequest));
    }

    // Validate scheduling time
    @Test
    void whenScheduledTimeInFuture_thenThrowException() {
        validRequest.setScheduledTime(LocalDateTime.now().plusHours(6));

        assertThrows(RuntimeException.class, () -> rideService.createRide(validRequest));
    }

    @Test
    void whenScheduledTimeExactlyFiveHours_thenThrowException() {
        validRequest.setScheduledTime(LocalDateTime.now().plusHours(5).plusMinutes(1));

        assertThrows(RuntimeException.class, () -> rideService.createRide(validRequest));
    }

    // Passenger not found
    @Test
    void whenPassengerNotFound_thenThrowException() {
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> rideService.createRide(validRequest));
    }

    // Passenger is blocked
    @Test
    void whenPassengerIsBlocked_thenThrowException() {
        passenger.setBlocked(true);
        passenger.setBlockReason("Smoking");
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rideService.createRide(validRequest));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Nalog je blokiran. Razlog: Smoking", exception.getReason());
    }

    // No availabl drivers
    @Test
    void whenNoDriverAvailable_thenRideStatusIsFailed() {
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(driverRepository.filterAvailableDrivers(any(), anyBoolean(), anyBoolean(), any())).thenReturn(Collections.emptyList());

        RideResponseDTO response = rideService.createRide(validRequest);

        assertEquals(RideStatus.FAILED, response.getStatus());
    }

    // Found available driver
    @Test
    void whenDriverAvailable_thenRideStatusIsScheduled() {
        Driver driver = new Driver();
        driver.setBlocked(false);
        driver.setScheduledRides(new ArrayList<>());
        driver.setFinishedRides(new ArrayList<>());
        driver.setActiveRide(null);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(driverRepository.filterAvailableDrivers(any(), anyBoolean(), anyBoolean(), any())).thenReturn(List.of(driver));
        when(rideRepository.findAllByDriverId(driver.getId())).thenReturn(List.of());
        when(guestRideRepository.findAllByDriverId(driver.getId())).thenReturn(List.of());

        RideResponseDTO response = rideService.createRide(validRequest);

        assertEquals(RideStatus.SCHEDULED, response.getStatus());
        assertEquals(750, response.getPrice());
    }

    @Test
    void whenSeveralDriversAreFree_thenNearestDriverIsAssigned() {
        Driver farDriver = driverAt(20L, 45.30, 19.95);
        Driver nearDriver = driverAt(21L, 45.01, 19.01);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(driverRepository.filterAvailableDrivers(any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(List.of(farDriver, nearDriver));
        when(rideRepository.findAllByDriverId(any())).thenReturn(List.of());
        when(guestRideRepository.findAllByDriverId(any())).thenReturn(List.of());
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId(90L);
            return ride;
        });

        RideResponseDTO response = rideService.createRide(validRequest);

        assertEquals(RideStatus.SCHEDULED, response.getStatus());
        assertSame(nearDriver, nearDriver.getScheduledRides().get(0).getDriver());
        assertEquals(1, nearDriver.getScheduledRides().size());
        assertEquals(0, farDriver.getScheduledRides().size());
        verify(notificationService).notify(nearDriver, nearDriver.getScheduledRides().get(0),
                "NEW_RIDE", "Dodeljena vam je nova vožnja od Ulica A do Ulica B.",
                "ride:90:assigned-driver:21");
    }

    // Driver is blocked
    @Test
    void whenAllDriversBlocked_thenRideStatusIsFailed() {
        Driver blockedDriver = new Driver();
        blockedDriver.setBlocked(true);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(driverRepository.filterAvailableDrivers(any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(List.of(blockedDriver));

        RideResponseDTO response = rideService.createRide(validRequest);

        assertEquals(RideStatus.FAILED, response.getStatus());
    }

    // Too many work hours
    @Test
    void whenDriverExceedsWorkLimit_thenRideStatusIsFailed() {
        Ride finishedRide = new Ride();
        finishedRide.setStartTime(LocalDateTime.now().minusHours(10));
        finishedRide.setEndTime(LocalDateTime.now().minusHours(1));
        Route route = new Route();
        route.setDuration(500);
        finishedRide.setRoute(route);

        Driver driver = new Driver();
        driver.setBlocked(false);
        driver.setFinishedRides(List.of(finishedRide));
        driver.setScheduledRides(new ArrayList<>());
        driver.setActiveRide(null);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(driverRepository.filterAvailableDrivers(any(), anyBoolean(), anyBoolean(), any())).thenReturn(List.of(driver));
        when(rideRepository.findAllByDriverId(driver.getId())).thenReturn(List.of(finishedRide));
        when(guestRideRepository.findAllByDriverId(driver.getId())).thenReturn(List.of());

        RideResponseDTO response = rideService.createRide(validRequest);

        assertEquals(RideStatus.FAILED, response.getStatus());
    }

    private Driver driverAt(long id, double latitude, double longitude) {
        Location location = new Location();
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        Vehicle vehicle = new Vehicle();
        vehicle.setLocation(location);

        Driver driver = new Driver();
        driver.setId(id);
        driver.setBlocked(false);
        driver.setStatus(DriverStatus.ACTIVE);
        driver.setVehicle(vehicle);
        driver.setScheduledRides(new ArrayList<>());
        driver.setFinishedRides(new ArrayList<>());
        return driver;
    }
}
