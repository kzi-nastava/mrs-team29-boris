package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.response.ActiveVehicleResponseDTO;
import com.example.backendspringboot.model.Vehicle;
import com.example.backendspringboot.repositories.VehicleRepository;
import com.example.backendspringboot.services.interfaces.VehicleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    private static final long IDLE_MOVEMENT_CYCLE_SECONDS = 120;
    private static final double IDLE_LATITUDE_RADIUS = 0.0012;
    private static final double IDLE_LONGITUDE_RADIUS = 0.0018;

    private final VehicleRepository vehicleRepository;

    public VehicleServiceImpl(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public List<ActiveVehicleResponseDTO> getAllActiveVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        return vehicles.stream()
                .filter(v -> v.getLocation() != null)
                .map(v -> new ActiveVehicleResponseDTO(
                        v.getId(),
                        getDisplayedLocation(v),
                        Boolean.TRUE.equals(v.getBusy())
                ))
                .collect(Collectors.toList());
    }

    private LocationDTO getDisplayedLocation(Vehicle vehicle) {
        double latitude = vehicle.getLocation().getLatitude();
        double longitude = vehicle.getLocation().getLongitude();

        // Free vehicles move slowly around their stored base location. The
        // position is derived from time, so simulation requires no database writes.
        if (!Boolean.TRUE.equals(vehicle.getBusy())) {
            double cycleProgress = (System.currentTimeMillis() / 1000L
                    % IDLE_MOVEMENT_CYCLE_SECONDS) / (double) IDLE_MOVEMENT_CYCLE_SECONDS;
            double vehicleOffset = ((vehicle.getId() == null ? 0L : vehicle.getId()) % 8) / 8.0;
            double angle = 2.0 * Math.PI * (cycleProgress + vehicleOffset);

            latitude += IDLE_LATITUDE_RADIUS * Math.sin(angle);
            longitude += IDLE_LONGITUDE_RADIUS * Math.cos(angle);
        }

        // LocationDTO constructor order is longitude, latitude, address.
        return new LocationDTO(longitude, latitude, vehicle.getLocation().getAddress());
    }
}
