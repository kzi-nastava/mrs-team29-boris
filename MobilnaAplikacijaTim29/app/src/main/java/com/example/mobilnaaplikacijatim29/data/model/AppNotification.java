package com.example.mobilnaaplikacijatim29.data.model;

public class AppNotification {
    private Long id;
    private Long rideId;
    private String type;
    private String content;
    private String createdAt;
    private boolean seen;

    public AppNotification() { }

    public AppNotification(Long id, Long rideId, String type, String content,
                           String createdAt, boolean seen) {
        this.id = id;
        this.rideId = rideId;
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
        this.seen = seen;
    }

    public Long getId() { return id; }
    public Long getRideId() { return rideId; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public boolean isSeen() { return seen; }
}
