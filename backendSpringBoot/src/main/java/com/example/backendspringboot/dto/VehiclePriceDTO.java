package com.example.backendspringboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePriceDTO {
    private double standardBasePrice;
    private double luxuryBasePrice;
    private double vanBasePrice;
    private double pricePerKm;
}