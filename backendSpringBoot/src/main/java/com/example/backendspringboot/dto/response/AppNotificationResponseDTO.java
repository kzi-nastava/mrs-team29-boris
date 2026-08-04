package com.example.backendspringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AppNotificationResponseDTO {
    private Long id;
    private Long rideId;
    private String type;
    private String content;
    private LocalDateTime createdAt;
    private boolean seen;
}
