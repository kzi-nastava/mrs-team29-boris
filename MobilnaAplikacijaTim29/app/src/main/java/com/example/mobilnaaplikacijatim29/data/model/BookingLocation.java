package com.example.mobilnaaplikacijatim29.data.model;

public class BookingLocation {
    private final Double longitude;
    private final Double latitude;
    private final String address;

    public BookingLocation(double longitude, double latitude, String address) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.address = address;
    }

    public Double getLongitude() { return longitude; }
    public Double getLatitude() { return latitude; }
    public String getAddress() { return address; }
}
