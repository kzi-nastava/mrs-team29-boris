package com.example.backendspringboot.services;

import com.example.backendspringboot.model.AppNotification;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.repositories.AppNotificationRepository;
import com.example.backendspringboot.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppNotificationServiceTest {
    private AppNotificationRepository repository;
    private UserRepository userRepository;
    private SimpMessagingTemplate messagingTemplate;
    private AppNotificationService service;
    private Passenger passenger;

    @BeforeEach
    void setUp() {
        repository = mock(AppNotificationRepository.class);
        userRepository = mock(UserRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new AppNotificationService(repository, userRepository, messagingTemplate);
        passenger = new Passenger();
        passenger.setId(4L);
    }

    @Test
    void notificationIsPersistedAndPublishedToRecipientTopic() {
        Ride ride = new Ride();
        ride.setId(9L);
        when(repository.existsByEventKey("ride:9:accepted:4")).thenReturn(false);
        when(repository.save(any(AppNotification.class))).thenAnswer(invocation -> {
            AppNotification value = invocation.getArgument(0);
            value.setId(12L);
            value.setCreatedAt(LocalDateTime.of(2026, 8, 25, 12, 0));
            return value;
        });

        var response = service.notify(passenger, ride, "RIDE_ACCEPTED",
                "Vožnja je prihvaćena.", "ride:9:accepted:4");

        assertEquals(12L, response.getId());
        assertEquals(9L, response.getRideId());
        verify(messagingTemplate).convertAndSend(
                eq("/topic/user/4/notifications"), any(Object.class));
    }

    @Test
    void repeatedEventKeyDoesNotCreateDuplicateReminder() {
        when(repository.existsByEventKey("same-key")).thenReturn(true);

        assertNull(service.notify(passenger, null, "RIDE_REMINDER", "Podsetnik", "same-key"));
        verify(repository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void openingHistoryMarksPersistedNotificationsAsSeen() {
        AppNotification first = notification(false);
        AppNotification second = notification(false);
        when(userRepository.findById(4L)).thenReturn(Optional.of(passenger));
        when(repository.findAllByRecipientIdOrderByCreatedAtDesc(4L))
                .thenReturn(List.of(first, second));

        service.markAllSeen(passenger);

        assertTrue(first.isSeen());
        assertTrue(second.isSeen());
        verify(repository).saveAll(List.of(first, second));
    }

    private AppNotification notification(boolean seen) {
        AppNotification value = new AppNotification();
        value.setRecipient(passenger);
        value.setSeen(seen);
        return value;
    }
}
