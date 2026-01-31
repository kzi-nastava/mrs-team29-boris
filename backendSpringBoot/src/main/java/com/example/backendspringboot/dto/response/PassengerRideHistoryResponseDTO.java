package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassengerRideHistoryResponseDTO {

    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocationDTO origin;
    private LocationDTO destination;
    private double totalPrice;
    private String driverEmail;
    private String status;
}
