package com.example.demo.services.interfaces;

import com.example.demo.dto.response.ActiveVehicleResponseDTO;
import java.util.List;

public interface VehicleService {
    List<ActiveVehicleResponseDTO> getAllActiveVehicles();
}