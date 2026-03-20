package com.example.mobilnaaplikacijatim29.data.model;

public class RegisterRequest {
    private final String name;
    private final String lastName;
    private final String email;
    private final String password;
    private final String confirmPassword;
    private final String address;
    private final String phoneNumber;
    private final boolean mobile;

    public RegisterRequest(String name, String lastName, String email, String password,
                           String confirmPassword, String address, String phoneNumber) {
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.mobile = true;
    }
}
