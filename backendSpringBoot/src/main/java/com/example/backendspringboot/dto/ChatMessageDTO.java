package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String message;
    private String senderEmail;
    private String receiverEmail;
    private LocalDateTime sentAt;
    private boolean seen;
}