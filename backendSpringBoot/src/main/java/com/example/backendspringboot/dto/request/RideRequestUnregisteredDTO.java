package com.example.demo.dto.request;

import com.example.demo.dto.LocationDTO;
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