package com.example.backendspringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SupportConversationResponseDTO {
    private Long userId;
    private String name;
    private String surname;
    private String email;
    private String role;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
