package com.example.mobilnaaplikacijatim29.data.model;

public class DriverProfileChangeResponse {
    private long requestId;
    private long driverId;
    private String driverEmail;
    private String createdAt;
    private ProfileResponse proposedProfile;

    public long getRequestId() { return requestId; }
    public long getDriverId() { return driverId; }
    public String getDriverEmail() { return driverEmail; }
    public String getCreatedAt() { return createdAt; }
    public ProfileResponse getProposedProfile() { return proposedProfile; }
}
