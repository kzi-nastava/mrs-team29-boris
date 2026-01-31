package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponseDTO {
    private Long id;
    private String model;
    private VehicleType type;
    private String registration;
    private int seats;
    private Boolean isBabyFriendly;
    private Boolean isPetFriendly;
}
