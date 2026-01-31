package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ScheduledRideResponseDTO {
    private Long id;
    private String origin;
    private String destination;
    private LocalDateTime scheduledTime;
    private boolean guest;
}
