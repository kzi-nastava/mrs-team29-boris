package com.example.mobilnaaplikacijatim29.ui.admin;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VehiclePricingFragmentTest {
    @Test
    public void acceptsPositiveFinitePriceWithinLimit() {
        assertTrue(VehiclePricingFragment.validPrice(0.01));
        assertTrue(VehiclePricingFragment.validPrice(1_000_000));
    }

    @Test
    public void rejectsInvalidPrices() {
        assertFalse(VehiclePricingFragment.validPrice(0));
        assertFalse(VehiclePricingFragment.validPrice(-1));
        assertFalse(VehiclePricingFragment.validPrice(Double.NaN));
        assertFalse(VehiclePricingFragment.validPrice(Double.POSITIVE_INFINITY));
        assertFalse(VehiclePricingFragment.validPrice(1_000_000.01));
    }
}
