package com.example.mobilnaaplikacijatim29.domain;

import com.example.mobilnaaplikacijatim29.data.model.BookingLocation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RideBookingCalculator {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private RideBookingCalculator() { }

    public static double routeDistanceKm(BookingLocation origin,
                                         List<BookingLocation> stops,
                                         BookingLocation destination) {
        if (origin == null || destination == null) return 0.0;
        List<BookingLocation> points = new ArrayList<>();
        points.add(origin);
        points.addAll(stops == null ? Collections.emptyList() : stops);
        points.add(destination);
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += haversine(points.get(i - 1), points.get(i));
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public static double price(double basePrice, double pricePerKm, double distanceKm) {
        return basePrice + distanceKm * pricePerKm;
    }

    public static int approximateDurationMinutes(double distanceKm) {
        return Math.max(1, (int) Math.ceil(distanceKm * 2.0));
    }

    public static boolean isAllowedSchedule(LocalDateTime now, LocalDateTime scheduledTime) {
        return scheduledTime != null && !scheduledTime.isBefore(now)
                && !scheduledTime.isAfter(now.plusHours(5));
    }

    public static LocalDateTime requestedStart(LocalDateTime now, boolean scheduledForLater,
                                               LocalDateTime selectedTime) {
        return scheduledForLater ? selectedTime : now;
    }

    private static double haversine(BookingLocation first, BookingLocation second) {
        double lat1 = Math.toRadians(first.getLatitude());
        double lat2 = Math.toRadians(second.getLatitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(second.getLongitude() - first.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }
}
