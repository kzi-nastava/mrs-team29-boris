package com.example.mobilnaaplikacijatim29.ui.driver;

import com.example.mobilnaaplikacijatim29.data.model.DriverRideHistoryItem;
import com.example.mobilnaaplikacijatim29.data.model.RideHistoryLocation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RideHistorySorterTest {
    private final RideHistoryLocation a = new RideHistoryLocation(19.8, 45.2, "A ruta");
    private final RideHistoryLocation b = new RideHistoryLocation(19.9, 45.3, "B ruta");

    @Test
    public void dateSortCanAlternateDirection() {
        DriverRideHistoryItem older = ride(1L, "2026-01-01T10:00:00", 900);
        DriverRideHistoryItem newer = ride(2L, "2026-02-01T10:00:00", 1200);
        List<DriverRideHistoryItem> rides = new ArrayList<>(List.of(older, newer));

        rides.sort(RideHistorySorter.comparator("Datum", false));
        assertEquals(Long.valueOf(2L), rides.get(0).getId());

        rides.sort(RideHistorySorter.comparator("Datum", true));
        assertEquals(Long.valueOf(1L), rides.get(0).getId());
    }

    @Test
    public void priceSortUsesNumericValue() {
        List<DriverRideHistoryItem> rides = new ArrayList<>(List.of(
                ride(1L, "2026-01-01T10:00:00", 1200),
                ride(2L, "2026-02-01T10:00:00", 900)));

        rides.sort(RideHistorySorter.comparator("Cena", true));

        assertEquals(Long.valueOf(2L), rides.get(0).getId());
    }

    private DriverRideHistoryItem ride(long id, String date, double price) {
        return new DriverRideHistoryItem(id, date, date, date, a, b,
                price, "FINISHED", false, null);
    }
}
