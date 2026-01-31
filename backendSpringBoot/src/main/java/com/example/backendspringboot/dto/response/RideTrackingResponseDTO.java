package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor  @AllArgsConstructor
public class RideTrackingResponseDTO {
    private Long rideId;
    private LocationDTO vehicleLocation;
    private int estimatedTimeInMinutes; // Updates through the ride

    private String status;
    private double progressPercent;
}