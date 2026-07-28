package com.example.mobilnaaplikacijatim29.data.model;

public class AdminReportUser {
    private Long id;
    private String username;
    private String role;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return username + " (" + ("DRIVER".equals(role) ? "vozač" : "putnik") + ")";
    }
}
