package com.example.backendspringboot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/// This class represents one chat with customer support.
/// Admins will be able to see all chats from users probably
/// Because of that, maybe this is not needed for him, but we'll see in further
/// implementation
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_sender_id")
    private List<Message> sentMessages = new ArrayList<>();;


    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_receiver_id")
    private List<Message> receivedMessages = new ArrayList<>();
}