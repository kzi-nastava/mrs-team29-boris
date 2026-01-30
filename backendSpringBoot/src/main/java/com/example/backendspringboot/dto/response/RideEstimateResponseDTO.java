package com.example.demo.dto.response;

import com.example.demo.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideEstimateResponseDTO {
    private List<LocationDTO> route;
    private int estimatedTime;
}
