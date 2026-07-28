package com.example.mobilnaaplikacijatim29.data.model;

public class DailyReportStats {
    private String date;
    private int numberOfRides;
    private double totalKilometers;
    private double totalMoney;
    private int cumulativeRides;
    private double cumulativeKilometers;
    private double cumulativeMoney;

    public String getDate() { return date; }
    public int getNumberOfRides() { return numberOfRides; }
    public double getTotalKilometers() { return totalKilometers; }
    public double getTotalMoney() { return totalMoney; }
    public int getCumulativeRides() { return cumulativeRides; }
    public double getCumulativeKilometers() { return cumulativeKilometers; }
    public double getCumulativeMoney() { return cumulativeMoney; }
}
