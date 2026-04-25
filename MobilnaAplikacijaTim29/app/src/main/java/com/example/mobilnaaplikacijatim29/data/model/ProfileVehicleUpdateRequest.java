package com.example.mobilnaaplikacijatim29.data.model;

public class ProfileVehicleUpdateRequest {
    private final String model;
    private final String type;
    private final String registration;
    private final int seats;
    private final boolean babyFriendly;
    private final boolean petFriendly;

    public ProfileVehicleUpdateRequest(String model, String type, String registration, int seats,
                                       boolean babyFriendly, boolean petFriendly) {
        this.model = model;
        this.type = type;
        this.registration = registration;
        this.seats = seats;
        this.babyFriendly = babyFriendly;
        this.petFriendly = petFriendly;
    }
}
