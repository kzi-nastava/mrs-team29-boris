package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.response.ActiveVehicleResponseDTO;
import java.util.List;

public interface VehicleService {
    List<ActiveVehicleResponseDTO> getAllActiveVehicles();
}