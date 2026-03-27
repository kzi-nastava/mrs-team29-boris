package com.example.mobilnaaplikacijatim29.data.model;

public class CompleteRegistrationRequest {
    private final String token;
    private final String password;
    private final String confirmPassword;

    public CompleteRegistrationRequest(String token, String password, String confirmPassword) {
        this.token = token;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }
}
