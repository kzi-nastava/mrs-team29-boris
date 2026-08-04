package com.example.mobilnaaplikacijatim29.data.model;

public class AppNotification {
    private Long id;
    private Long rideId;
    private String type;
    private String content;
    private String createdAt;
    private boolean seen;

    public Long getId() { return id; }
    public Long getRideId() { return rideId; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public boolean isSeen() { return seen; }
}
