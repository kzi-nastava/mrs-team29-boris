package com.example.mobilnaaplikacijatim29.domain;

import com.example.mobilnaaplikacijatim29.data.model.BookingLocation;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RideBookingCalculatorTest {

    @Test
    public void routeDistanceIncludesStopsInTheirGivenOrder() {
        BookingLocation origin = point(19.80, 45.25);
        BookingLocation stop = point(19.85, 45.27);
        BookingLocation destination = point(19.90, 45.29);

        double direct = RideBookingCalculator.routeDistanceKm(origin, List.of(), destination);
        double withStop = RideBookingCalculator.routeDistanceKm(origin, List.of(stop), destination);

        assertTrue(withStop >= direct);
        assertTrue(withStop > 0.0);
    }

    @Test
    public void priceUsesConfiguredBaseAndPerKilometreValues() {
        assertEquals(750.0, RideBookingCalculator.price(150.0, 120.0, 5.0), 0.001);
    }

    @Test
    public void durationHasMinimumOfOneMinute() {
        assertEquals(1, RideBookingCalculator.approximateDurationMinutes(0.1));
        assertEquals(11, RideBookingCalculator.approximateDurationMinutes(5.2));
    }

    @Test
    public void scheduleMustBeBetweenNowAndFiveHoursAhead() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 10, 0);

        assertTrue(RideBookingCalculator.isAllowedSchedule(now, now));
        assertTrue(RideBookingCalculator.isAllowedSchedule(now, now.plusHours(5)));
        assertFalse(RideBookingCalculator.isAllowedSchedule(now, now.minusSeconds(1)));
        assertFalse(RideBookingCalculator.isAllowedSchedule(now, now.plusHours(5).plusSeconds(1)));
    }

    private BookingLocation point(double longitude, double latitude) {
        return new BookingLocation(longitude, latitude, "test");
    }
}
