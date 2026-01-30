package com.example.demo.dto.response;

import com.example.demo.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}