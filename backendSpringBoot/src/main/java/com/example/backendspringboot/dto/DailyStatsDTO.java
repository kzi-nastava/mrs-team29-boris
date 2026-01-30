package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class DailyStatsDTO {
    private LocalDate date;
    private int numberOfRides;
    private double totalKilometers;
    private double totalMoney;

    private int cumulativeRides;
    private double cumulativeKilometers;
    private double cumulativeMoney;
}
