package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteFromFavoritesResponseDTO {
    private Long id;
    private LocationDTO origin;
    private LocationDTO destination;
    private double distance;
    private int duration;
    private int timesUsed;
    private List<LocationDTO> stops;
}
