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

import java.util.Optional;

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
}
