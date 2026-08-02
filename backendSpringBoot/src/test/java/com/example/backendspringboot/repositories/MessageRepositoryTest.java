package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan(basePackages = "com.example.backendspringboot.model")
class MessageRepositoryTest {
    @Autowired TestEntityManager entityManager;
    @Autowired MessageRepository messageRepository;

    @Test
    void supportHistoryIncludesMessagesFromDifferentAdministratorsInChronologicalOrder() {
        Passenger passenger = persistUser(new Passenger(), "passenger-chat@test.com");
        passenger.setActivated(true);
        Administrator firstAdmin = persistUser(new Administrator(), "admin-chat-1@test.com");
        Administrator secondAdmin = persistUser(new Administrator(), "admin-chat-2@test.com");
        Chat chat = new Chat();
        chat.setUser(passenger);
        chat = entityManager.persistFlushFind(chat);
        entityManager.persistAndFlush(message(chat, passenger, firstAdmin, "Prva poruka"));
        entityManager.persistAndFlush(message(chat, secondAdmin, passenger, "Druga poruka"));

        List<Message> result = messageRepository.findSupportMessagesForUser(passenger.getId());

        assertEquals(List.of("Prva poruka", "Druga poruka"),
                result.stream().map(Message::getContent).toList());
        assertEquals(2, messageRepository.findAllSupportMessages().size());
    }

    private <T extends User> T persistUser(T user, String email) {
        user.setEmail(email);
        user.setPassword("Password1");
        user.setName("Test");
        user.setSurname("Korisnik");
        user.setGender(Gender.MALE);
        user.setAddress("Test adresa");
        user.setPhone("060000000");
        return entityManager.persistFlushFind(user);
    }

    private Message message(Chat chat, User from, User to, String content) {
        Message message = new Message();
        message.setChat(chat);
        message.setFrom(from);
        message.setTo(to);
        message.setContent(content);
        return message;
    }
}
