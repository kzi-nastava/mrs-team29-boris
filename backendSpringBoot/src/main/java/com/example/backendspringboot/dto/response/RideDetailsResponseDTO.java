package com.example.demo.dto.response;

import com.example.demo.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideDetailsResponseDTO {
    private Long rideId;
    private String startAddress;
    private String endAddress;

    private LocationDTO origin;
    private LocationDTO destination;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean canceled;
    private String canceledBy;
    private double price;
    private boolean panicTriggered;
    private String status;

    private Long driverId;
    private String driverName;

    private List<UserProfileResponseDTO> passengers;

    private Integer rating;
    private String comment;
}