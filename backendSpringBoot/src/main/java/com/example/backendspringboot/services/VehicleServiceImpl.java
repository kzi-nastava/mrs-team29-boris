package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.response.ActiveVehicleResponseDTO;
import com.example.backendspringboot.model.Vehicle;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.DriverStatus;
import com.example.backendspringboot.dto.response.RideTrackingResponseDTO;
import com.example.backendspringboot.repositories.DriverRepository;
import com.example.backendspringboot.repositories.VehicleRepository;
import com.example.backendspringboot.services.interfaces.VehicleService;
import com.example.backendspringboot.services.interfaces.RideService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final RideService rideService;
    private final IdleVehiclePositionService idleVehiclePositionService;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              DriverRepository driverRepository,
                              RideService rideService,
                              IdleVehiclePositionService idleVehiclePositionService) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.rideService = rideService;
        this.idleVehiclePositionService = idleVehiclePositionService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveVehicleResponseDTO> getAllActiveVehicles() {
        return driverRepository.findAllByStatus(DriverStatus.ACTIVE).stream()
                .map(Driver::getVehicle)
                .filter(v -> v != null && v.getLocation() != null)
                .map(v -> new ActiveVehicleResponseDTO(
                        v.getId(),
                        getDisplayedLocation(v),
                        Boolean.TRUE.equals(v.getBusy())
                ))
                .collect(Collectors.toList());
    }

    private LocationDTO getDisplayedLocation(Vehicle vehicle) {
        if (Boolean.TRUE.equals(vehicle.getBusy())) {
            Driver driver = driverRepository.findByVehicleId(vehicle.getId()).orElse(null);
            if (driver != null && driver.getActiveRide() != null) {
                RideTrackingResponseDTO tracking =
                        rideService.getRideTracking(driver.getActiveRide().getId());
                if (tracking.getVehicleLocation() != null) return tracking.getVehicleLocation();
            }
        } else {
            return idleVehiclePositionService.currentLocation(vehicle);
        }
        return new LocationDTO(vehicle.getLocation().getLongitude(),
                vehicle.getLocation().getLatitude(), vehicle.getLocation().getAddress());
    }
}
