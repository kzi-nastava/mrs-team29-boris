package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.RideRequestUnregisteredDTO;
import com.example.backendspringboot.dto.response.GuestRideResponseDTO;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.DriverStatus;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.repositories.DriverRepository;
import com.example.backendspringboot.repositories.GuestRideRepository;
import com.example.backendspringboot.repositories.LocationRepository;
import com.example.backendspringboot.repositories.RouteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestRideBlockingServiceTest {
    @Mock GuestRideRepository guestRideRepository;
    @Mock LocationRepository locationRepository;
    @Mock RouteRepository routeRepository;
    @Mock DriverRepository driverRepository;
    @InjectMocks GuestRideServiceImpl service;

    @Test
    void blockedDriverIsNotAssignedToGuestRide() {
        Driver blockedDriver = new Driver();
        blockedDriver.setStatus(DriverStatus.ACTIVE);
        blockedDriver.setBlocked(true);
        when(driverRepository.findAll()).thenReturn(List.of(blockedDriver));

        RideRequestUnregisteredDTO request = new RideRequestUnregisteredDTO();
        request.setOrigin(new LocationDTO(19.80, 45.24, "Polazište"));
        request.setDestination(new LocationDTO(19.85, 45.27, "Odredište"));

        GuestRideResponseDTO result = service.createGuestRide(request);

        assertEquals(RideStatus.CREATED, result.getStatus());
    }
}
