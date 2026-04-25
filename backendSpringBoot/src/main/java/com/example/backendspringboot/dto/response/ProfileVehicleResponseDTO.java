package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileVehicleResponseDTO {
    private Long id;
    private String model;
    private VehicleType type;
    private String registration;
    private int seats;
    private boolean babyFriendly;
    private boolean petFriendly;
}
