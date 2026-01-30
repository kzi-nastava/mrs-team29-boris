package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SummaryStatsDTO {
    private int totalRides;
    private double totalKilometers;
    private double totalMoney;
    private double avgRidesPerDay;
    private double avgKilometersPerDay;
    private double avgMoneyPerDay;
}
