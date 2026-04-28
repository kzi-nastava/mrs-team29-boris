package com.example.mobilnaaplikacijatim29.data.model;

public class InconsistencyReport {
    private Long id;
    private String note;
    private String createdAt;
    private String passengerEmail;
    private Long rideId;

    public Long getId() { return id; }
    public String getNote() { return note; }
    public String getCreatedAt() { return createdAt; }
    public String getPassengerEmail() { return passengerEmail; }
    public Long getRideId() { return rideId; }
}
