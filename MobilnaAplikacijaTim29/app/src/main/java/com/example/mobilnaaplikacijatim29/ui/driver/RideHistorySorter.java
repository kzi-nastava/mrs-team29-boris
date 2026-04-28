package com.example.mobilnaaplikacijatim29.ui.driver;

import com.example.mobilnaaplikacijatim29.data.model.DriverRideHistoryItem;
import com.example.mobilnaaplikacijatim29.data.model.RideHistoryLocation;

import java.util.Comparator;

final class RideHistorySorter {
    private RideHistorySorter() { }

    static Comparator<DriverRideHistoryItem> comparator(String field, boolean ascending) {
        Comparator<DriverRideHistoryItem> comparator;
        switch (field) {
            case "Ruta":
                comparator = Comparator.comparing(RideHistorySorter::route,
                        String.CASE_INSENSITIVE_ORDER);
                break;
            case "Početak":
                comparator = Comparator.comparing(item -> safe(item.getStartTime()));
                break;
            case "Kraj":
                comparator = Comparator.comparing(item -> safe(item.getEndTime()));
                break;
            case "Cena":
                comparator = Comparator.comparingDouble(DriverRideHistoryItem::getTotalPrice);
                break;
            case "Status":
                comparator = Comparator.comparing(item -> safe(item.getStatus()),
                        String.CASE_INSENSITIVE_ORDER);
                break;
            case "Otkazivanje":
                comparator = Comparator.comparing(DriverRideHistoryItem::isCanceled)
                        .thenComparing(item -> safe(item.getCanceledBy()),
                                String.CASE_INSENSITIVE_ORDER);
                break;
            default:
                comparator = Comparator.comparing(RideHistorySorter::historyDate);
        }
        return ascending ? comparator : comparator.reversed();
    }

    static String historyDate(DriverRideHistoryItem item) {
        return item.getStartTime() == null ? safe(item.getCreatedAt()) : item.getStartTime();
    }

    private static String route(DriverRideHistoryItem item) {
        return address(item.getOrigin()) + " " + address(item.getDestination());
    }

    private static String address(RideHistoryLocation location) {
        return location == null ? "" : safe(location.getAddress());
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
