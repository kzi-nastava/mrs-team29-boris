package com.example.mobilnaaplikacijatim29.data.model;

public class ProfileVehicle {
    private Long id;
    private String model;
    private String type;
    private String registration;
    private int seats;
    private boolean babyFriendly;
    private boolean petFriendly;

    public Long getId() { return id; }
    public String getModel() { return model; }
    public String getType() { return type; }
    public String getRegistration() { return registration; }
    public int getSeats() { return seats; }
    public boolean isBabyFriendly() { return babyFriendly; }
    public boolean isPetFriendly() { return petFriendly; }
}
