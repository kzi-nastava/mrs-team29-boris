package com.example.mobilnaaplikacijatim29.data.model;

public class ActiveVehicleResponse {
    private Long id;
    private LocationResponse currentLocation;
    private boolean busy;

    public Long getId() {
        return id;
    }

    public LocationResponse getCurrentLocation() {
        return currentLocation;
    }

    public boolean isBusy() {
        return busy;
    }
}
