package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PriceDefinitionRequestDTO {
    private String vehicleType; // STANDARD, LUXURY, VAN
    private double pricePerKm;
}
