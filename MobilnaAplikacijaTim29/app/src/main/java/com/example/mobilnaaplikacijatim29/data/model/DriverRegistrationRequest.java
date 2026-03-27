package com.example.mobilnaaplikacijatim29.data.model;

public class DriverRegistrationRequest {
    private final String email;
    private final String name;
    private final String surname;
    private final String gender;
    private final String address;
    private final String phone;
    private final VehicleRegistrationRequest vehicle;

    public DriverRegistrationRequest(String email, String name, String surname, String gender,
                                     String address, String phone,
                                     VehicleRegistrationRequest vehicle) {
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.gender = gender;
        this.address = address;
        this.phone = phone;
        this.vehicle = vehicle;
    }
}
