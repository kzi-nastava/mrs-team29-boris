package com.example.demo.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RideHistoryResponseDTO {
    private Long rideId;
    private String startAddress;
    private String endAddress;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean canceled;
    private String canceledBy;
    private double price;
    private boolean panicTriggered;
}
