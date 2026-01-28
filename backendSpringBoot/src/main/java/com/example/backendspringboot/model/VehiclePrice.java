package com.example.backendspringboot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class VehiclePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double standard;

    @Column(nullable = false)
    private double luxury;

    @Column(nullable = false)
    private double van;

    @Column(nullable = false, name = "price_per_km")
    private double perKm;
}