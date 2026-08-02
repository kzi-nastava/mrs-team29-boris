package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.SupportMessageRequestDTO;
import com.example.backendspringboot.dto.response.SupportConversationResponseDTO;
import com.example.backendspringboot.dto.response.SupportMessageResponseDTO;
import com.example.backendspringboot.model.Administrator;
import com.example.backendspringboot.model.Chat;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.Message;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.repositories.AdministratorRepository;
import com.example.backendspringboot.repositories.ChatRepository;
import com.example.backendspringboot.repositories.MessageRepository;
import com.example.backendspringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupportChatService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AdministratorRepository administratorRepository;
    private final ChatRepository chatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public List<SupportMessageResponseDTO> history(User requester, Long requestedUserId) {
        User conversationUser = conversationUser(requester, requestedUserId);
        List<Message> messages = messageRepository.findSupportMessagesForUser(conversationUser.getId());
        boolean changed = false;
        for (Message message : messages) {
            boolean shouldMarkSeen = requester instanceof Administrator
                    ? message.getFrom().getId().equals(conversationUser.getId())
                    : message.getTo().getId().equals(requester.getId());
            if (shouldMarkSeen && !message.isSeen()) {
                message.setSeen(true);
                changed = true;
            }
        }
        if (changed) messageRepository.saveAll(messages);
        return messages.stream().map(this::mapMessage).toList();
    }

    @Transactional
    public SupportMessageResponseDTO send(User sender, SupportMessageRequestDTO request) {
        requireSupportedPrincipal(sender);
        User conversationUser;
        User receiver;
        if (sender instanceof Administrator) {
            conversationUser = requireNonAdmin(request.getUserId());
            receiver = conversationUser;
        } else {
            conversationUser = sender;
            receiver = administratorRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "No support administrator is available"));
        }

        Message message = new Message();
        message.setChat(requireChat(conversationUser));
        message.setFrom(sender);
        message.setTo(receiver);
        message.setContent(request.getMessage().trim());
        message.setSeen(false);
        message.setSentAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        messagingTemplate.convertAndSend("/topic/support/" + conversationUser.getId(),
                Map.of("type", "SUPPORT_MESSAGE", "userId", conversationUser.getId(),
                        "messageId", saved.getId() == null ? -1L : saved.getId()));
        return mapMessage(saved);
    }

    private Chat requireChat(User conversationUser) {
        return chatRepository.findByUserId(conversationUser.getId()).orElseGet(() -> {
            Chat chat = new Chat();
            chat.setUser(conversationUser);
            return chatRepository.save(chat);
        });
    }

    @Transactional(readOnly = true)
    public List<SupportConversationResponseDTO> conversations(User requester) {
        if (!(requester instanceof Administrator)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Map<Long, ConversationAccumulator> conversations = new LinkedHashMap<>();
        for (Message message : messageRepository.findAllSupportMessages()) {
            User user = message.getFrom() instanceof Administrator ? message.getTo() : message.getFrom();
            ConversationAccumulator item = conversations.computeIfAbsent(user.getId(), ignored ->
                    new ConversationAccumulator(user));
            item.lastMessage = message.getContent();
            item.lastMessageAt = message.getSentAt();
            if (!message.isSeen() && message.getFrom().getId().equals(user.getId())) {
                item.unreadCount++;
            }
        }
        return conversations.values().stream()
                .sorted((a, b) -> compareNullableDesc(a.lastMessageAt, b.lastMessageAt))
                .map(ConversationAccumulator::toResponse)
                .toList();
    }

    private User conversationUser(User requester, Long requestedUserId) {
        requireSupportedPrincipal(requester);
        if (requester instanceof Administrator) return requireNonAdmin(requestedUserId);
        if (requestedUserId != null && !requester.getId().equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return requester;
    }

    private User requireNonAdmin(Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "User id is required for administrator support chat");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user instanceof Administrator) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return user;
    }

    private void requireSupportedPrincipal(User user) {
        if (!(user instanceof Administrator) && !(user instanceof Driver)
                && !(user instanceof Passenger)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private SupportMessageResponseDTO mapMessage(Message message) {
        User sender = message.getFrom();
        return new SupportMessageResponseDTO(message.getId(), sender.getId(), sender.getEmail(),
                sender.getName() + " " + sender.getSurname(), role(sender), message.getContent(),
                message.getSentAt(), message.isSeen());
    }

    private static String role(User user) {
        return user instanceof Administrator ? "admin"
                : user instanceof Driver ? "driver" : "user";
    }

    private static int compareNullableDesc(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? 0 : 1;
        if (second == null) return -1;
        return second.compareTo(first);
    }

    private static final class ConversationAccumulator {
        private final User user;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private long unreadCount;

        private ConversationAccumulator(User user) { this.user = user; }

        private SupportConversationResponseDTO toResponse() {
            return new SupportConversationResponseDTO(user.getId(), user.getName(), user.getSurname(),
                    user.getEmail(), role(user), lastMessage, lastMessageAt, unreadCount);
        }
    }
}
