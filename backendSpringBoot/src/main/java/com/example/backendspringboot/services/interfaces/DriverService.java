package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.request.CompleteRegistrationRequestDTO;
import com.example.backendspringboot.dto.request.DriverRegistrationRequestDTO;
import com.example.backendspringboot.dto.response.DriverRegistrationResponseDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import com.example.backendspringboot.dto.response.VehicleResponseDTO;
import com.example.backendspringboot.dto.response.DriverRideHistoryResponseDTO;
import com.example.backendspringboot.model.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverService {
    DriverRegistrationResponseDTO registerDriver(DriverRegistrationRequestDTO request, String platform);
    void completeRegistration(CompleteRegistrationRequestDTO request);
    boolean isTokenValid(String token);
    VehicleResponseDTO getDriverVehicle(Long id);
    List<DriverRideHistoryResponseDTO> getDriverRideHistory(Long driverId);

    boolean isOwnerOrAdmin(Long id);
    List<UserProfileResponseDTO> getAllDrivers();
}
