package com.example.mobilnaaplikacijatim29.data.model;

public class RideReviewRequest {
    private final int driverRating;
    private final int vehicleRating;
    private final String comment;

    public RideReviewRequest(int driverRating, int vehicleRating, String comment) {
        this.driverRating = driverRating;
        this.vehicleRating = vehicleRating;
        this.comment = comment;
    }
}
