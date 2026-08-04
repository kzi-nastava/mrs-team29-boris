package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.response.AppNotificationResponseDTO;
import com.example.backendspringboot.model.AppNotification;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.repositories.AppNotificationRepository;
import com.example.backendspringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppNotificationService {
    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public AppNotificationResponseDTO notify(User recipient, Ride ride, String type,
                                             String content, String eventKey) {
        if (notificationRepository.existsByEventKey(eventKey)) return null;
        AppNotification notification = new AppNotification();
        notification.setRecipient(recipient);
        notification.setRide(ride);
        notification.setType(type);
        notification.setContent(content);
        notification.setEventKey(eventKey);
        notification.setSeen(false);
        AppNotification saved = notificationRepository.save(notification);
        AppNotificationResponseDTO response = map(saved);
        messagingTemplate.convertAndSend("/topic/user/" + recipient.getId() + "/notifications",
                response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<AppNotificationResponseDTO> list(User principal) {
        User user = requireUser(principal);
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::map).toList();
    }

    @Transactional
    public void markAllSeen(User principal) {
        User user = requireUser(principal);
        List<AppNotification> notifications =
                notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(user.getId());
        notifications.forEach(notification -> notification.setSeen(true));
        notificationRepository.saveAll(notifications);
    }

    private User requireUser(User principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private AppNotificationResponseDTO map(AppNotification notification) {
        return new AppNotificationResponseDTO(notification.getId(),
                notification.getRide() == null ? null : notification.getRide().getId(),
                notification.getType(), notification.getContent(),
                notification.getCreatedAt(), notification.isSeen());
    }
}
