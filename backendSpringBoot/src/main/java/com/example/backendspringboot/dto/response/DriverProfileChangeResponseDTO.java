package com.example.backendspringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DriverProfileChangeResponseDTO {
    private Long requestId;
    private Long driverId;
    private String driverEmail;
    private LocalDateTime createdAt;
    private OwnProfileResponseDTO proposedProfile;
}
