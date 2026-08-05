package com.example.backendspringboot.dto.request;

import com.example.backendspringboot.dto.LocationDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoutePreviewRequestDTO {
    @NotNull
    @Valid
    private LocationDTO origin;

    @Valid
    private List<LocationDTO> stops;

    @NotNull
    @Valid
    private LocationDTO destination;
}
