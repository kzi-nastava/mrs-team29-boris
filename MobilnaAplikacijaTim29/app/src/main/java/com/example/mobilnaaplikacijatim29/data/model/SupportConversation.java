package com.example.mobilnaaplikacijatim29.data.model;

public class SupportConversation {
    private Long userId;
    private String name;
    private String surname;
    private String email;
    private String role;
    private String lastMessage;
    private String lastMessageAt;
    private long unreadCount;

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageAt() { return lastMessageAt; }
    public long getUnreadCount() { return unreadCount; }
}
