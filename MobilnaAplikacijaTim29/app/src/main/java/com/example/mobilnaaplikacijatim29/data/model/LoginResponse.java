package com.example.mobilnaaplikacijatim29.data.model;

public class LoginResponse {
    private Long userId;
    private String email;
    private String role;
    private String token;
    private boolean blocked;
    private String blockReason;

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getBlockReason() {
        return blockReason;
    }
}
