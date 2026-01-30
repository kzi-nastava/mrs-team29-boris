package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ChatMessageResponseDTO {
    private Long id;
    private String senderEmail;
    private String message;
    private LocalDateTime timestamp;
}