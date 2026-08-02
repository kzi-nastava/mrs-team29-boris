package com.example.mobilnaaplikacijatim29.data.model;

public class VehiclePriceConfig {
    private double standardBasePrice;
    private double luxuryBasePrice;
    private double vanBasePrice;
    private double pricePerKm;

    public VehiclePriceConfig(double standardBasePrice, double luxuryBasePrice,
                              double vanBasePrice, double pricePerKm) {
        this.standardBasePrice = standardBasePrice;
        this.luxuryBasePrice = luxuryBasePrice;
        this.vanBasePrice = vanBasePrice;
        this.pricePerKm = pricePerKm;
    }

    public double getStandardBasePrice() { return standardBasePrice; }
    public double getLuxuryBasePrice() { return luxuryBasePrice; }
    public double getVanBasePrice() { return vanBasePrice; }
    public double getPricePerKm() { return pricePerKm; }
}
