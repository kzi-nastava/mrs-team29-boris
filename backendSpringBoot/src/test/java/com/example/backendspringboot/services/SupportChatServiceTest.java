package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.SupportMessageRequestDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.AdministratorRepository;
import com.example.backendspringboot.repositories.ChatRepository;
import com.example.backendspringboot.repositories.MessageRepository;
import com.example.backendspringboot.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SupportChatServiceTest {
    private MessageRepository messageRepository;
    private UserRepository userRepository;
    private AdministratorRepository administratorRepository;
    private ChatRepository chatRepository;
    private SupportChatService service;
    private Administrator admin;
    private Passenger passenger;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        userRepository = mock(UserRepository.class);
        administratorRepository = mock(AdministratorRepository.class);
        chatRepository = mock(ChatRepository.class);
        service = new SupportChatService(messageRepository, userRepository,
                administratorRepository, chatRepository, mock(SimpMessagingTemplate.class));
        admin = user(new Administrator(), 1L, "admin@demo.com", "Admin");
        passenger = user(new Passenger(), 2L, "passenger@demo.com", "Ana");
    }

    @Test
    void passengerMessageUsesAuthenticatedIdentityAndSupportAdministrator() {
        SupportMessageRequestDTO request = request(null, "  Potrebna mi je pomoć  ");
        when(administratorRepository.findAll()).thenReturn(List.of(admin));
        stubExistingChat();
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(10L);
            return message;
        });

        var result = service.send(passenger, request);

        assertEquals(passenger.getId(), result.getSenderId());
        assertEquals("Potrebna mi je pomoć", result.getMessage());
        verify(messageRepository).save(argThat(message ->
                message.getChat().getUser() == passenger
                        && message.getFrom() == passenger && message.getTo() == admin));
    }

    @Test
    void administratorCanReplyToSelectedUser() {
        SupportMessageRequestDTO request = request(2L, "Odgovor podrške");
        when(userRepository.findById(2L)).thenReturn(Optional.of(passenger));
        stubExistingChat();
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.send(admin, request);

        assertEquals("admin", result.getSenderRole());
        verify(messageRepository).save(argThat(message ->
                message.getChat().getUser() == passenger
                        && message.getFrom() == admin && message.getTo() == passenger));
    }

    @Test
    void passengerCannotOpenAnotherUsersConversation() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.history(passenger, 99L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(messageRepository);
    }

    @Test
    void openingHistoryMarksIncomingMessagesAsSeen() {
        Message incoming = message(5L, admin, passenger, "Odgovor", false, 10);
        Message outgoing = message(6L, passenger, admin, "Hvala", false, 11);
        when(messageRepository.findSupportMessagesForUser(2L))
                .thenReturn(List.of(incoming, outgoing));

        var result = service.history(passenger, null);

        assertTrue(incoming.isSeen());
        assertFalse(outgoing.isSeen());
        assertEquals(2, result.size());
        verify(messageRepository).saveAll(anyList());
    }

    @Test
    void administratorInboxCombinesRepliesFromAllAdminsIntoOneConversation() {
        Administrator secondAdmin = user(new Administrator(), 3L, "admin2@demo.com", "Drugi");
        Message question = message(1L, passenger, admin, "Pitanje", false, 10);
        Message reply = message(2L, secondAdmin, passenger, "Odgovor", false, 11);
        when(messageRepository.findAllSupportMessages()).thenReturn(List.of(question, reply));

        var conversations = service.conversations(admin);

        assertEquals(1, conversations.size());
        assertEquals(passenger.getId(), conversations.get(0).getUserId());
        assertEquals("Odgovor", conversations.get(0).getLastMessage());
        assertEquals(1, conversations.get(0).getUnreadCount());
    }

    private SupportMessageRequestDTO request(Long userId, String text) {
        SupportMessageRequestDTO request = new SupportMessageRequestDTO();
        request.setUserId(userId);
        request.setMessage(text);
        return request;
    }

    private void stubExistingChat() {
        Chat chat = new Chat();
        chat.setUser(passenger);
        when(chatRepository.findByUserId(passenger.getId())).thenReturn(Optional.of(chat));
    }

    private Message message(long id, User from, User to, String text, boolean seen, int minute) {
        Message message = new Message();
        message.setId(id);
        message.setFrom(from);
        message.setTo(to);
        message.setContent(text);
        message.setSeen(seen);
        message.setSentAt(LocalDateTime.of(2026, 8, 25, 10, minute));
        return message;
    }

    private <T extends User> T user(T user, long id, String email, String name) {
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setSurname("Test");
        return user;
    }
}
