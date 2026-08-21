package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ActiveVehicleResponseDTO {
    private Long id;
    private String driverName;
    private LocationDTO currentLocation;
    private boolean busy; // true if busy
}
