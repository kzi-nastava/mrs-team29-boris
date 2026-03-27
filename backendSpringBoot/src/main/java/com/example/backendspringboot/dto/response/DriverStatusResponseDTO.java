package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DriverStatusResponseDTO {
    private DriverStatus status;
    private boolean deactivateAfterRide;
    private boolean activeRide;
}
