package com.example.mobilnaaplikacijatim29.data.model;

import java.util.Collections;
import java.util.List;

public class ScheduledRide {
    private long id;
    private String origin;
    private String destination;
    private String scheduledTime;
    private Long secondsUntilStart;
    private boolean guest;
    private List<RidePassenger> passengers;

    public long getId() { return id; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getScheduledTime() { return scheduledTime; }
    public Long getSecondsUntilStart() { return secondsUntilStart; }
    public boolean isGuest() { return guest; }
    public List<RidePassenger> getPassengers() {
        return passengers == null ? Collections.emptyList() : passengers;
    }
}
