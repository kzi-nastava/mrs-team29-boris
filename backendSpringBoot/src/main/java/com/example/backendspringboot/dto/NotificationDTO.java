package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String content;
    private Long rideId;
    private String timestamp;

    public NotificationDTO(String content, Long rideId) {
        this.content = content;
        this.rideId = rideId;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public String getContent() {
        return content;
    }

    public Long getRideId() {
        return rideId;
    }

    public String getTimestamp() {
        return timestamp;
    }
}



