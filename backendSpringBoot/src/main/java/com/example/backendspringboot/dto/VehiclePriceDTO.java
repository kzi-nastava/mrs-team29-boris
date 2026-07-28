package com.example.backendspringboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePriceDTO {
    @Positive @DecimalMax("1000000")
    private double standardBasePrice;
    @Positive @DecimalMax("1000000")
    private double luxuryBasePrice;
    @Positive @DecimalMax("1000000")
    private double vanBasePrice;
    @Positive @DecimalMax("1000000")
    private double pricePerKm;
}
