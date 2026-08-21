package com.example.mobilnaaplikacijatim29.domain;

import java.util.Locale;

public final class RideStartCountdown {
    private RideStartCountdown() { }

    public static boolean canStart(long remainingSeconds) {
        return remainingSeconds <= 0L;
    }

    public static String format(long remainingSeconds) {
        long safeSeconds = Math.max(0L, remainingSeconds);
        long hours = safeSeconds / 3600L;
        long minutes = (safeSeconds % 3600L) / 60L;
        long seconds = safeSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }
}
