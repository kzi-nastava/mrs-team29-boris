package com.example.mobilnaaplikacijatim29.ui.report;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReportChartViewTest {
    @Test
    public void chartMaximumUsesLargestValue() {
        assertEquals(12.5, ReportChartView.chartMaximum(List.of(0.0, 12.5, 4.0)), 0.001);
    }

    @Test
    public void chartMaximumAvoidsZeroScale() {
        assertEquals(1.0, ReportChartView.chartMaximum(List.of(0.0, 0.0)), 0.001);
    }
}
