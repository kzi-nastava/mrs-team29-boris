package com.example.demo.dto.request;

import com.example.demo.dto.LocationDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideStopRequestDTO {

    @NotNull(message = "Stop location is required")
    @Valid
    private LocationDTO stopLocation;

    @NotNull
    private Boolean guest;
}