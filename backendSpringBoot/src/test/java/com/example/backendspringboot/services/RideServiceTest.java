package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.RideStopRequestDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import com.example.backendspringboot.services.interfaces.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RideServiceTest {
    @Mock
    private RideRepository rideRepository;

    @Mock
    private GuestRideRepository guestRideRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AppNotificationService notificationService;

    @InjectMocks
    private RideServiceImpl rideService;

    private Ride ride;
    private Driver driver;
    private Vehicle vehicle;
    private RideStopRequestDTO dto;

    @BeforeEach
    void setup() {

        vehicle = new Vehicle();
        vehicle.setBusy(true);

        driver = new Driver();
        driver.setId(10L);
        driver.setEmail("driver@example.com");
        driver.setVehicle(vehicle);

        ride = new Ride();
        ride.setId(1L);
        ride.setStatus(RideStatus.STARTED);
        ride.setDriver(driver);
        ride.setRoute(new Route());

        driver.setActiveRide(ride);

        dto = new RideStopRequestDTO();
        LocationDTO stopLoc = new LocationDTO();
        stopLoc.setLatitude(45.0);
        stopLoc.setLongitude(19.0);
        stopLoc.setAddress("Test address");
        dto.setStopLocation(stopLoc);
        dto.setGuest(false);
    }

    @Test
    void stopRegularRide_success() {

        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(locationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        rideService.stopRide(1L, dto);

        assertEquals(RideStatus.STOPPED, ride.getStatus());
        assertNull(driver.getActiveRide());
        assertFalse(vehicle.getBusy());

        verify(rideRepository).save(ride);
        verify(driverRepository).save(driver);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void stopRegularRide_notStarted_throwsException() {

        ride.setStatus(RideStatus.CREATED);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThrows(IllegalStateException.class,
                () -> rideService.stopRide(1L, dto));
    }

    @Test
    void stopRegularRide_notFound() {

        when(rideRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> rideService.stopRide(1L, dto));
    }

    @Test
    void assignedDriverStartsScheduledRide() {
        Passenger creator = new Passenger();
        creator.setId(20L);
        ride.setRideCreator(creator);
        ride.setStatus(RideStatus.SCHEDULED);
        driver.setActiveRide(null);
        vehicle.setBusy(false);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        rideService.startRide(1L, false, "driver@example.com");

        assertEquals(RideStatus.STARTED, ride.getStatus());
        assertNotNull(ride.getStartTime());
        assertSame(ride, driver.getActiveRide());
        assertTrue(vehicle.getBusy());
        verify(rideRepository).save(ride);
        verify(driverRepository).save(driver);
        verify(vehicleRepository).save(vehicle);
        verify(notificationService).notify(creator, ride, "RIDE_STARTED",
                "Vožnja #1 je započeta.", "ride:1:started:20");
    }

    @Test
    void differentDriverCannotStartAssignedRide() {
        ride.setStatus(RideStatus.SCHEDULED);
        driver.setActiveRide(null);
        vehicle.setBusy(false);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rideService.startRide(1L, false, "other@example.com"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(RideStatus.SCHEDULED, ride.getStatus());
        assertNull(ride.getStartTime());
    }

    @Test
    void rideCannotBeStartedTwice() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rideService.startRide(1L, false, "driver@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void differentDriverCannotFinishActiveRide() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rideService.finishRide(1L, "other@example.com", 10, false));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(RideStatus.STARTED, ride.getStatus());
    }

    @Test
    void assignedDriverCannotFinishBeforeReachingDestination() {
        ride.getRoute().setDuration(10);
        ride.setStartTime(LocalDateTime.now());
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rideService.finishRide(1L, "driver@example.com", 10, false));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Vožnja može da se završi tek kada vozilo stigne na odredište.",
                exception.getReason());
        assertEquals(RideStatus.STARTED, ride.getStatus());
        assertSame(ride, driver.getActiveRide());
        assertTrue(vehicle.getBusy());
    }
}
