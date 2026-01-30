package com.example.demo.dto.request;

import com.example.demo.model.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleRegistrationRequestDTO {
    @NotNull(message = "Vehicle model cannot be null")
    @NotBlank(message = "Vehicle model cannot be blank")
    private String model;

    @NotNull(message = "Vehicle type cannot be null")
    private VehicleType type;

    @NotNull(message = "Vehicle registration cannot be null")
    @NotBlank(message = "Vehicle registration cannot be blank")
    private String registration;

    @Min(4)
    @Max(12)
    private int seats;

    private boolean isBabyFriendly;
    private boolean isPetFriendly;
}
