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
    private LocationResponse origin;
    private LocationResponse destination;
    private List<LocationResponse> stops;
    private double price;
    private boolean canReview;
    private boolean alreadyReviewed;
    private String reviewDeadline;

    public Long getRideId() { return rideId; }
    public LocationResponse getVehicleLocation() { return vehicleLocation; }
    public int getEstimatedTimeInMinutes() { return estimatedTimeInMinutes; }
    public String getStatus() { return status; }
    public double getProgressPercent() { return progressPercent; }
    public LocationResponse getOrigin() { return origin; }
    public LocationResponse getDestination() { return destination; }
    public List<LocationResponse> getStops() {
        return stops == null ? Collections.emptyList() : stops;
    }
    public double getPrice() { return price; }
    public boolean canReview() { return canReview; }
    public boolean isAlreadyReviewed() { return alreadyReviewed; }
    public String getReviewDeadline() { return reviewDeadline; }
    public List<LocationResponse> getRouteGeometry() {
        return routeGeometry == null ? Collections.emptyList() : routeGeometry;
    }
}
