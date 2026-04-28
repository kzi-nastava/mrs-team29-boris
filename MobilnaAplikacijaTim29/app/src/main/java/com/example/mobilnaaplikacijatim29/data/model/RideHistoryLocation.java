package com.example.mobilnaaplikacijatim29.data.model;

public class RideHistoryLocation {
    private Double longitude;
    private Double latitude;
    private String address;

    public RideHistoryLocation() { }

    public RideHistoryLocation(Double longitude, Double latitude, String address) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.address = address;
    }

    public Double getLongitude() { return longitude; }
    public Double getLatitude() { return latitude; }
    public String getAddress() { return address; }
}
