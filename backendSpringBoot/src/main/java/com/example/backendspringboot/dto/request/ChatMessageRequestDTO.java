package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ChatMessageRequestDTO {
    private String message;
    private String receiverEmail;
    private String senderEmail;
    private LocalDateTime sentAt;
}