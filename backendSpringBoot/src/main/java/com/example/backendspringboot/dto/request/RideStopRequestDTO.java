package com.example.backendspringboot.dto.request;

import com.example.backendspringboot.dto.LocationDTO;
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