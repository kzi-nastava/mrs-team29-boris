package com.example.mobilnaaplikacijatim29.notifications;

import com.example.mobilnaaplikacijatim29.data.model.AppNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Selects database notifications which have not yet been shown by Android.
 * The database is recreated during development, so a monotonically increasing
 * database ID alone is not a stable delivery marker.
 */
public final class NotificationDeliveryTracker {
    private NotificationDeliveryTracker() { }

    public static List<AppNotification> pending(List<AppNotification> values,
                                                Set<String> deliveredFingerprints) {
        if (values == null || values.isEmpty()) return Collections.emptyList();

        List<AppNotification> pending = new ArrayList<>();
        // The API returns newest first; Android should display them chronologically.
        for (int i = values.size() - 1; i >= 0; i--) {
            AppNotification value = values.get(i);
            if (value == null || value.isSeen()) continue;
            if (!deliveredFingerprints.contains(fingerprint(value))) pending.add(value);
        }
        return pending;
    }

    public static String fingerprint(AppNotification value) {
        return part(value.getId()) + "|" + part(value.getCreatedAt()) + "|"
                + part(value.getRideId()) + "|" + part(value.getType()) + "|"
                + part(value.getContent());
    }

    private static String part(Object value) {
        return value == null ? "" : value.toString();
    }
}
