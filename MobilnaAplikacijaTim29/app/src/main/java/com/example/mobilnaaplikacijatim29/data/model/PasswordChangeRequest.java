package com.example.mobilnaaplikacijatim29.data.model;

public class PasswordChangeRequest {
    private final String currentPassword;
    private final String newPassword;
    private final String confirmPassword;

    public PasswordChangeRequest(String currentPassword, String newPassword,
                                 String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}
