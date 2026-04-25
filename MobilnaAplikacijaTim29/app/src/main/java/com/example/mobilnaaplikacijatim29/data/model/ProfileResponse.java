package com.example.mobilnaaplikacijatim29.data.model;

public class ProfileResponse {
    private Long id;
    private String email;
    private String name;
    private String surname;
    private String gender;
    private String address;
    private String phone;
    private String profileImageUrl;
    private String role;
    private Integer activeMinutesLast24Hours;
    private ProfileVehicle vehicle;
    private boolean profileChangePending;

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getGender() { return gender; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getRole() { return role; }
    public Integer getActiveMinutesLast24Hours() { return activeMinutesLast24Hours; }
    public ProfileVehicle getVehicle() { return vehicle; }
    public boolean isProfileChangePending() { return profileChangePending; }
}
