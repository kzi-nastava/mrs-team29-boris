package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RoutePreviewResponseDTO {
    private double distanceKm;
    private int durationMinutes;
    private List<LocationDTO> geometry;
}
