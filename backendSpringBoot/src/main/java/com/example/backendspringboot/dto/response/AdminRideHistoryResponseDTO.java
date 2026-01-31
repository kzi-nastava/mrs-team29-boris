package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.LocationDTO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminRideHistoryResponseDTO {

    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocationDTO origin;
    private LocationDTO destination;

    private double totalPrice;

    private boolean panicPressed;

    private boolean cancelled;
    private String cancelledBy;

    private String driverEmail;
    private List<String> passengerEmails;

    private String status;
}
