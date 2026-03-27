package com.example.mobilnaaplikacijatim29.data.model;

public class ResetPasswordRequest {
    private final String token;
    private final String newPassword;
    private final String confirmPassword;

    public ResetPasswordRequest(String token, String newPassword, String confirmPassword) {
        this.token = token;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}
