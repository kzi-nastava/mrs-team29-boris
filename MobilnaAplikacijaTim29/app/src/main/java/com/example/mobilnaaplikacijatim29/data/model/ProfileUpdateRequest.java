package com.example.mobilnaaplikacijatim29.data.model;

public class ProfileUpdateRequest {
    private final String name;
    private final String surname;
    private final String email;
    private final String gender;
    private final String address;
    private final String phone;
    private final ProfileVehicleUpdateRequest vehicle;

    public ProfileUpdateRequest(String name, String surname, String email, String gender,
                                String address, String phone, ProfileVehicleUpdateRequest vehicle) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.gender = gender;
        this.address = address;
        this.phone = phone;
        this.vehicle = vehicle;
    }
}
