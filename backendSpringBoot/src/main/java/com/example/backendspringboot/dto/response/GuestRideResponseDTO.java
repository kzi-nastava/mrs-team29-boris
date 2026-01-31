package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GuestRideResponseDTO {
    private Long id;
    private RideStatus status;
    private int estimatedTimeMinutes;
    private double distanceKm;
}
