package com.example.backendspringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RideReviewResponseDTO {
    private String passengerEmail;
    private int driverRating;
    private int vehicleRating;
    private String comment;
    private LocalDateTime createdAt;
}
