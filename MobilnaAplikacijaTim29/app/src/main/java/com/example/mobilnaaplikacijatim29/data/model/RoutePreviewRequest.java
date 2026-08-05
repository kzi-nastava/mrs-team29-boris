package com.example.mobilnaaplikacijatim29.data.model;

import java.util.List;

public class RoutePreviewRequest {
    private final BookingLocation origin;
    private final List<BookingLocation> stops;
    private final BookingLocation destination;

    public RoutePreviewRequest(BookingLocation origin, List<BookingLocation> stops,
                               BookingLocation destination) {
        this.origin = origin;
        this.stops = stops;
        this.destination = destination;
    }
}
