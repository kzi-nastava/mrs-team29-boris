package com.example.mobilnaaplikacijatim29.data.model;

import java.util.List;

public class CreateRideRequest {
    private final BookingLocation origin;
    private final BookingLocation destination;
    private final List<BookingLocation> stops;
    private final List<String> passengerEmails;
    private final String vehicleType;
    private final String scheduledTime;
    private final boolean babyFriendly;
    private final boolean petFriendly;
    private final int durationMinutes;
    private final double distanceKm;

    public CreateRideRequest(BookingLocation origin, BookingLocation destination,
                             List<BookingLocation> stops, List<String> passengerEmails,
                             String vehicleType, String scheduledTime,
                             boolean babyFriendly, boolean petFriendly,
                             int durationMinutes, double distanceKm) {
        this.origin = origin;
        this.destination = destination;
        this.stops = stops;
        this.passengerEmails = passengerEmails;
        this.vehicleType = vehicleType;
        this.scheduledTime = scheduledTime;
        this.babyFriendly = babyFriendly;
        this.petFriendly = petFriendly;
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
    }
}
