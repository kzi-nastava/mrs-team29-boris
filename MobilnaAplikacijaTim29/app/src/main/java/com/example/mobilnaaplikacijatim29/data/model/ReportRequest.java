package com.example.mobilnaaplikacijatim29.data.model;

public class ReportRequest {
    private final String dateFrom;
    private final String dateTo;
    private final Long userId;
    private final String userType;

    public ReportRequest(String dateFrom, String dateTo, Long userId, String userType) {
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.userId = userId;
        this.userType = userType;
    }
}
