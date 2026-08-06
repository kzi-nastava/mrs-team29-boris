package com.example.mobilnaaplikacijatim29.data.model;

import java.util.Collections;
import java.util.List;

public class RideTrackingResponse {
    private Long rideId;
    private LocationResponse vehicleLocation;
    private int estimatedTimeInMinutes;
    private String status;
    private double progressPercent;
    private List<LocationResponse> routeGeometry;

    public Long getRideId() { return rideId; }
    public LocationResponse getVehicleLocation() { return vehicleLocation; }
    public int getEstimatedTimeInMinutes() { return estimatedTimeInMinutes; }
    public String getStatus() { return status; }
    public double getProgressPercent() { return progressPercent; }
    public List<LocationResponse> getRouteGeometry() {
        return routeGeometry == null ? Collections.emptyList() : routeGeometry;
    }
}
