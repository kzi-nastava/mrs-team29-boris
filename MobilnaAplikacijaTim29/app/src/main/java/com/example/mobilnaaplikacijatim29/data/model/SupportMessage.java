package com.example.mobilnaaplikacijatim29.data.model;

public class SupportMessage {
    private Long id;
    private Long senderId;
    private String senderEmail;
    private String senderName;
    private String senderRole;
    private String message;
    private String sentAt;
    private boolean seen;

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public String getSenderEmail() { return senderEmail; }
    public String getSenderName() { return senderName; }
    public String getSenderRole() { return senderRole; }
    public String getMessage() { return message; }
    public String getSentAt() { return sentAt; }
    public boolean isSeen() { return seen; }
}
