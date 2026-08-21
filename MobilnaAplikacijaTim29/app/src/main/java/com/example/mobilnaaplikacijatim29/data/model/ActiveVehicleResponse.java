package com.example.mobilnaaplikacijatim29.data.model;

public class ActiveVehicleResponse {
    private Long id;
    private String driverName;
    private LocationResponse currentLocation;
    private boolean busy;

    public Long getId() {
        return id;
    }

    public String getDriverName() {
        return driverName;
    }

    public LocationResponse getCurrentLocation() {
        return currentLocation;
    }

    public boolean isBusy() {
        return busy;
    }
}
