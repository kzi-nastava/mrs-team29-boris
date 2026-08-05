package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.response.ActiveVehicleResponseDTO;
import com.example.backendspringboot.model.Location;
import com.example.backendspringboot.model.Vehicle;
import com.example.backendspringboot.repositories.VehicleRepository;
import com.example.backendspringboot.repositories.DriverRepository;
import com.example.backendspringboot.services.interfaces.RideService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleServiceTest {

    @Test
    void returnsCoordinatesInLongitudeLatitudeOrderNearStoredLocation() {
        VehicleRepository repository = mock(VehicleRepository.class);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setBusy(false);
        vehicle.setLocation(new Location(
                1L,
                19.8302,
                45.2641,
                "Bulevar Kralja Petra I"
        ));
        when(repository.findAll()).thenReturn(List.of(vehicle));
        IdleVehiclePositionService idlePositions = mock(IdleVehiclePositionService.class);
        when(idlePositions.currentLocation(vehicle)).thenReturn(
                new com.example.backendspringboot.dto.LocationDTO(
                        19.8302, 45.2641, "Bulevar Kralja Petra I"));

        ActiveVehicleResponseDTO result = new VehicleServiceImpl(repository,
                mock(DriverRepository.class), mock(RideService.class), idlePositions)
                .getAllActiveVehicles()
                .get(0);

        assertEquals(1L, result.getId());
        assertFalse(result.isBusy());
        assertEquals("Bulevar Kralja Petra I", result.getCurrentLocation().getAddress());
        assertEquals(45.2641, result.getCurrentLocation().getLatitude());
        assertEquals(19.8302, result.getCurrentLocation().getLongitude());
    }

    @Test
    void doesNotSimulateIdleMovementForBusyVehicle() {
        VehicleRepository repository = mock(VehicleRepository.class);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setBusy(true);
        vehicle.setLocation(new Location(2L, 19.8820, 45.2426, "Futoški put"));
        when(repository.findAll()).thenReturn(List.of(vehicle));

        ActiveVehicleResponseDTO result = new VehicleServiceImpl(repository,
                mock(DriverRepository.class), mock(RideService.class),
                mock(IdleVehiclePositionService.class))
                .getAllActiveVehicles()
                .get(0);

        assertEquals(19.8820, result.getCurrentLocation().getLongitude());
        assertEquals(45.2426, result.getCurrentLocation().getLatitude());
    }
}
