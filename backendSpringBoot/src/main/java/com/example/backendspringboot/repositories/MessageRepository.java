package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE (m.from.email = :e1 AND m.to.email = :e2) OR (m.from.email = :e2 AND m.to.email = :e1) ORDER BY m.sentAt ASC")
    List<Message> findAllChatMessagesByEmails(String e1, String e2);

    @Query("SELECT DISTINCT m.from.email FROM Message m WHERE m.to.email = :adminEmail")
    List<String> findUserEmailsWhoChatedWithAdmin(String adminEmail);
}