package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findAllByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    boolean existsByEventKey(String eventKey);
}
