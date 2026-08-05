package com.example.mobilnaaplikacijatim29.data.model;

import java.util.List;

public class RoutePreviewResponse {
    private double distanceKm;
    private int durationMinutes;
    private List<BookingLocation> geometry;

    public double getDistanceKm() { return distanceKm; }
    public int getDurationMinutes() { return durationMinutes; }
    public List<BookingLocation> getGeometry() { return geometry; }
}
