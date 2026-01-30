package com.example.demo.dto.request;

import com.example.demo.model.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVehicleRequestDTO {

    @NotBlank(message = "Vehicle model cannot be blank")
    private String model;

    @NotNull(message = "Vehicle type cannot be blank")
    private VehicleType type;

    @NotBlank(message = "Vehicle registration cannot be blank")
    private String registration;

    private boolean isBabyFriendly;
    private boolean isPetFriendly;

    public boolean getIsBabyFriendly() {
        return isBabyFriendly;
    }
    public boolean getIsPetFriendly() {
        return isPetFriendly;
    }
}
