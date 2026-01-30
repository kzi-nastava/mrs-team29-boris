package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PanicResponseDTO {
    private Long id;
    private Long rideId;
    private boolean isGuest;
    private String triggeredByName;
    private String triggeredByEmail;
    private LocalDateTime createdAt;
    private boolean resolved;
    private String originAddress;
    private String destinationAddress;
}