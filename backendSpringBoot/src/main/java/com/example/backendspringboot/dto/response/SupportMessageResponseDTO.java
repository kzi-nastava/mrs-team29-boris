package com.example.backendspringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SupportMessageResponseDTO {
    private Long id;
    private Long senderId;
    private String senderEmail;
    private String senderName;
    private String senderRole;
    private String message;
    private LocalDateTime sentAt;
    private boolean seen;
}
