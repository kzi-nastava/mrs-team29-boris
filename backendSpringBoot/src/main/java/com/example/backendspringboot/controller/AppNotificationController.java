package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.response.AppNotificationResponseDTO;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.services.AppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class AppNotificationController {
    private final AppNotificationService notificationService;

    @GetMapping
    public List<AppNotificationResponseDTO> list(Authentication authentication) {
        return notificationService.list(principal(authentication));
    }

    @PostMapping("/seen")
    public ResponseEntity<Void> markAllSeen(Authentication authentication) {
        notificationService.markAllSeen(principal(authentication));
        return ResponseEntity.noContent().build();
    }

    private User principal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof User user
                ? user : null;
    }
}
