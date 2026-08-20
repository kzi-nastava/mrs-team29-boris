package com.example.mobilnaaplikacijatim29.notifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.mobilnaaplikacijatim29.data.model.AppNotification;

import org.junit.Test;

public class SystemNotificationHelperTest {
    @Test
    public void rideStartedNotificationOpensRideTracking() {
        AppNotification notification = new AppNotification(7L, 42L, "RIDE_STARTED",
                "Vožnja je započeta.", "2026-08-25T12:00:00", false);

        assertTrue(SystemNotificationHelper.opensRide(notification));
    }

    @Test
    public void rejectedRideWithoutTrackingTargetOpensNotificationList() {
        AppNotification notification = new AppNotification(8L, null, "RIDE_REJECTED",
                "Vožnja nije prihvaćena.", "2026-08-25T12:01:00", false);

        assertFalse(SystemNotificationHelper.opensRide(notification));
    }
}
