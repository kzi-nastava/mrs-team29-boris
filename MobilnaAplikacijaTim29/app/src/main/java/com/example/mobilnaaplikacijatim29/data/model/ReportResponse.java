package com.example.mobilnaaplikacijatim29.data.model;

import java.util.Collections;
import java.util.List;

public class ReportResponse {
    private List<DailyReportStats> dailyStats;
    private ReportSummary summary;
    private boolean earnings;

    public List<DailyReportStats> getDailyStats() {
        return dailyStats == null ? Collections.emptyList() : dailyStats;
    }
    public ReportSummary getSummary() { return summary; }
    public boolean isEarnings() { return earnings; }
}
