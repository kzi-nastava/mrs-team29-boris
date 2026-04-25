package com.example.backendspringboot.dto.request;

import com.example.backendspringboot.model.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileVehicleUpdateRequestDTO {
    @NotBlank
    private String model;
    @NotNull
    private VehicleType type;
    @NotBlank
    private String registration;
    @Min(4) @Max(12)
    private int seats;
    private boolean babyFriendly;
    private boolean petFriendly;
}
