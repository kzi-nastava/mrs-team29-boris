package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.response.ActiveVehicleResponseDTO;
import com.example.backendspringboot.dto.VehiclePriceDTO;
import com.example.backendspringboot.services.interfaces.VehicleService;
import com.example.backendspringboot.services.interfaces.VehiclePriceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "http://localhost:4200")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehiclePriceService vehiclePriceService;


    public VehicleController(VehicleService vehicleService, VehiclePriceService vehiclePriceService) {
        this.vehicleService = vehicleService;
        this.vehiclePriceService = vehiclePriceService;
    }

    // 2.1.1: Start view, all vehicles
    @GetMapping("/active")
    public ResponseEntity<List<ActiveVehicleResponseDTO>> getActiveVehicles() {
        List<ActiveVehicleResponseDTO> activeVehicles = vehicleService.getAllActiveVehicles();
        return ResponseEntity.ok(activeVehicles);
    }

    @GetMapping("/prices")
    @PreAuthorize("hasRole('USER')")
    public VehiclePriceDTO getPrices() {
        return vehiclePriceService.getPrices();
    }
}
