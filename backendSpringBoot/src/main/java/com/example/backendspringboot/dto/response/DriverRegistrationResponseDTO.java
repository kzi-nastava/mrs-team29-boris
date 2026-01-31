package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DriverRegistrationResponseDTO {
    private Long id;
    private String email;
    private String name;
    private String surname;
    private DriverStatus status;
}
