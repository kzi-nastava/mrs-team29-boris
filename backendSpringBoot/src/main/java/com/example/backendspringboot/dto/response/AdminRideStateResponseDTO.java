package com.example.demo.dto.response;

import com.example.demo.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AdminRideStateResponseDTO {
    private Long rideId;
    private String driverEmail;
    private List<String> passengerEmails;
    private LocationDTO currentLocation;
    private String originAddress;
    private String destinationAddress;
    private LocalDateTime startTime;
    private String status;
}
