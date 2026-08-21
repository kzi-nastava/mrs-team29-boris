package com.example.mobilnaaplikacijatim29.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RideStartCountdownTest {
    @Test
    public void formatsDemoCountdown() {
        assertEquals("01:10", RideStartCountdown.format(70));
        assertEquals("00:09", RideStartCountdown.format(9));
        assertEquals("1:00:01", RideStartCountdown.format(3601));
    }

    @Test
    public void startIsAllowedOnlyWhenCountdownExpires() {
        assertFalse(RideStartCountdown.canStart(70));
        assertFalse(RideStartCountdown.canStart(1));
        assertTrue(RideStartCountdown.canStart(0));
    }
}
