package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor  @AllArgsConstructor
public class RideTrackingResponseDTO {
    private Long rideId;
    private LocationDTO vehicleLocation;
    private int estimatedTimeInMinutes; // Updates through the ride

    private String status;
    private double progressPercent;
    private List<LocationDTO> routeGeometry;
    private LocationDTO origin;
    private LocationDTO destination;
    private List<LocationDTO> stops;
    private double price;
    private boolean canReview;
    private boolean alreadyReviewed;
    private LocalDateTime reviewDeadline;
}
