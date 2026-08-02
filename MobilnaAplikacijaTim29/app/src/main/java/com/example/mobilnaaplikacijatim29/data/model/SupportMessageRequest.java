package com.example.mobilnaaplikacijatim29.data.model;

public class SupportMessageRequest {
    private final Long userId;
    private final String message;

    public SupportMessageRequest(Long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public String getMessage() { return message; }
}
