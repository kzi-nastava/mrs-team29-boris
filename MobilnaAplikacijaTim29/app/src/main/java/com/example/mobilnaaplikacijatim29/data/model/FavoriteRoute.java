package com.example.mobilnaaplikacijatim29.data.model;

import java.util.Collections;
import java.util.List;

public class FavoriteRoute {
    private Long id;
    private LocationResponse origin;
    private LocationResponse destination;
    private double distance;
    private int duration;
    private int timesUsed;
    private List<LocationResponse> stops;

    public Long getId() { return id; }
    public LocationResponse getOrigin() { return origin; }
    public LocationResponse getDestination() { return destination; }
    public double getDistance() { return distance; }
    public int getDuration() { return duration; }
    public int getTimesUsed() { return timesUsed; }
    public List<LocationResponse> getStops() {
        return stops == null ? Collections.emptyList() : stops;
    }
}
