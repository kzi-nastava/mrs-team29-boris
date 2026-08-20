package com.example.mobilnaaplikacijatim29.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.example.mobilnaaplikacijatim29.data.model.AppNotification;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationDeliveryTrackerTest {
    @Test
    public void databaseResetWithReusedIdStillProducesNewNotification() {
        AppNotification beforeReset = notification(1L, "2026-08-24T10:00:00", false);
        AppNotification afterReset = notification(1L, "2026-08-25T10:00:00", false);
        Set<String> delivered = Set.of(NotificationDeliveryTracker.fingerprint(beforeReset));

        assertNotEquals(NotificationDeliveryTracker.fingerprint(beforeReset),
                NotificationDeliveryTracker.fingerprint(afterReset));
        assertEquals(List.of(afterReset),
                NotificationDeliveryTracker.pending(List.of(afterReset), delivered));
    }

    @Test
    public void deliveredAndAlreadySeenNotificationsAreNotRepeated() {
        AppNotification deliveredValue = notification(4L, "2026-08-25T10:00:00", false);
        AppNotification seenValue = notification(5L, "2026-08-25T10:01:00", true);
        Set<String> delivered = new HashSet<>();
        delivered.add(NotificationDeliveryTracker.fingerprint(deliveredValue));

        assertEquals(List.of(), NotificationDeliveryTracker.pending(
                List.of(seenValue, deliveredValue), delivered));
    }

    private static AppNotification notification(Long id, String createdAt, boolean seen) {
        return new AppNotification(id, 12L, "RIDE_ACCEPTED",
                "Vožnja je prihvaćena.", createdAt, seen);
    }
}
