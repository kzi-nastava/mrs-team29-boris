package com.example.mobilnaaplikacijatim29.data.model;

public class BlockableUser {
    private Long id;
    private String email;
    private String name;
    private String surname;
    private String address;
    private String phone;
    private String profileImageUrl;
    private boolean blocked;
    private String blockReason;

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public boolean isBlocked() { return blocked; }
    public String getBlockReason() { return blockReason; }
}
