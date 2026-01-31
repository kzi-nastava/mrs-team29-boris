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
                        new LocationDTO(
                                v.getLocation().getLatitude(),
                                v.getLocation().getLongitude(),
                                v.getLocation().getAddress()
                        ),
                        Boolean.TRUE.equals(v.getBusy())
                ))
                .collect(Collectors.toList());
    }
}