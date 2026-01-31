package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RideResponseDTO {
    private Long id;
    private RideStatus status;
    private double price;
}
