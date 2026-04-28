package com.example.mobilnaaplikacijatim29.data.model;

import java.util.Collections;
import java.util.List;

public class DriverRideHistoryItem {
    private Long id;
    private String createdAt;
    private String startTime;
    private String endTime;
    private RideHistoryLocation origin;
    private RideHistoryLocation destination;
    private double totalPrice;
    private List<InconsistencyReport> inconsistencyReports;
    private List<RidePassenger> passengers;
    private List<RideReview> reviews;
    private boolean panicPressed;
    private String status;
    private boolean canceled;
    private String canceledBy;
    private String cancellationReason;
    private boolean guest;

    public DriverRideHistoryItem() { }

    public DriverRideHistoryItem(Long id, String createdAt, String startTime, String endTime,
                                 RideHistoryLocation origin, RideHistoryLocation destination,
                                 double totalPrice, String status, boolean canceled,
                                 String canceledBy) {
        this.id = id;
        this.createdAt = createdAt;
        this.startTime = startTime;
        this.endTime = endTime;
        this.origin = origin;
        this.destination = destination;
        this.totalPrice = totalPrice;
        this.status = status;
        this.canceled = canceled;
        this.canceledBy = canceledBy;
    }

    public Long getId() { return id; }
    public String getCreatedAt() { return createdAt; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public RideHistoryLocation getOrigin() { return origin; }
    public RideHistoryLocation getDestination() { return destination; }
    public double getTotalPrice() { return totalPrice; }
    public List<InconsistencyReport> getInconsistencyReports() {
        return inconsistencyReports == null ? Collections.emptyList() : inconsistencyReports;
    }
    public List<RidePassenger> getPassengers() {
        return passengers == null ? Collections.emptyList() : passengers;
    }
    public List<RideReview> getReviews() { return reviews == null ? Collections.emptyList() : reviews; }
    public boolean isPanicPressed() { return panicPressed; }
    public String getStatus() { return status; }
    public boolean isCanceled() { return canceled; }
    public String getCanceledBy() { return canceledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public boolean isGuest() { return guest; }
}
