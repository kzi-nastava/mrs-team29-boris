package com.example.backendspringboot.dto.request;

import com.example.backendspringboot.dto.LocationDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideRequestUnregisteredDTO {
    @NotNull(message = "Origin is required")
    @Valid
    private LocationDTO origin;

    @NotNull(message = "Destination is required")
    @Valid
    private LocationDTO destination;
}