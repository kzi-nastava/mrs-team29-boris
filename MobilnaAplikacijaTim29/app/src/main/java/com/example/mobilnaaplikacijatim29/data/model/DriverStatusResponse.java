package com.example.mobilnaaplikacijatim29.data.model;

public class DriverStatusResponse {
    private String status;
    private boolean deactivateAfterRide;
    private boolean activeRide;

    public String getStatus() {
        return status;
    }

    public boolean isDeactivateAfterRide() {
        return deactivateAfterRide;
    }

    public boolean hasActiveRide() {
        return activeRide;
    }
}
