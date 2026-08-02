package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.request.SupportMessageRequestDTO;
import com.example.backendspringboot.dto.response.SupportConversationResponseDTO;
import com.example.backendspringboot.dto.response.SupportMessageResponseDTO;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.services.SupportChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/support")
@RequiredArgsConstructor
public class ChatController {
    private final SupportChatService supportChatService;

    @GetMapping("/messages")
    public List<SupportMessageResponseDTO> history(Authentication authentication,
                                                   @RequestParam(required = false) Long userId) {
        return supportChatService.history(principal(authentication), userId);
    }

    @PostMapping("/messages")
    public SupportMessageResponseDTO send(Authentication authentication,
                                          @Valid @RequestBody SupportMessageRequestDTO request) {
        return supportChatService.send(principal(authentication), request);
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SupportConversationResponseDTO> conversations(Authentication authentication) {
        return supportChatService.conversations(principal(authentication));
    }

    private User principal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof User user
                ? user : null;
    }
}
