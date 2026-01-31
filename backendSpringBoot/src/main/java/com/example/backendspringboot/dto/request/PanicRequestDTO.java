package com.example.backendspringboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PanicRequestDTO {
    private Long rideId;
    private boolean guest;
    private Long userId;
}