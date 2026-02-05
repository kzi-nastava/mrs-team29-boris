package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.ChatMessageDTO;
import com.example.backendspringboot.model.Message;
import com.example.backendspringboot.repositories.MessageRepository;
import com.example.backendspringboot.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    private final MessageRepository messageRepository;
    private final UserService userService;

    @GetMapping("/{userEmail:.+}/{adminEmail:.+}")
    @PreAuthorize("hasAnyRole('USER','DRIVER','ADMIN')")
    public ResponseEntity<List<ChatMessageDTO>> getHistory(@PathVariable String userEmail, @PathVariable String adminEmail) {
        List<Message> messages = messageRepository.findAllChatMessagesByEmails(userEmail, adminEmail);

        return ResponseEntity.ok(messages.stream().map(m -> new ChatMessageDTO(
                m.getContent(), m.getFrom().getEmail(), m.getTo().getEmail(), m.getSentAt(), m.isSeen()
        )).toList());
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('USER','DRIVER','ADMIN')")
    public ResponseEntity<ChatMessageDTO> sendMessage(@RequestBody ChatMessageDTO dto) {
        Message m = new Message();
        m.setFrom(userService.findByEmail(dto.getSenderEmail()));
        m.setTo(userService.findByEmail(dto.getReceiverEmail()));
        m.setContent(dto.getMessage());
        m.setSeen(false);
        m.setSentAt(LocalDateTime.now());

        messageRepository.save(m);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/admin/users/{adminEmail:.+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getChatUsers(@PathVariable String adminEmail) {
        return ResponseEntity.ok(messageRepository.findUserEmailsWhoChatedWithAdmin(adminEmail));
    }

    @PutMapping("/seen/{receiverEmail:.+}/{senderEmail:.+}")
    @PreAuthorize("hasAnyRole('USER','DRIVER','ADMIN')")
    public ResponseEntity<Void> markAsSeen(@PathVariable String receiverEmail, @PathVariable String senderEmail) {
        List<Message> messages = messageRepository.findAllChatMessagesByEmails(receiverEmail, senderEmail);

        for (Message m : messages) {
            if (m.getTo().getEmail().equals(receiverEmail) && !m.isSeen()) {
                m.setSeen(true);
            }
        }
        messageRepository.saveAll(messages);
        return ResponseEntity.ok().build();
    }
}