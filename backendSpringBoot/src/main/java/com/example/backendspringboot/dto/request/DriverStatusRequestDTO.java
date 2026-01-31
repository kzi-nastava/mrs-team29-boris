package com.example.backendspringboot.dto.request;

import com.example.backendspringboot.model.DriverStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverStatusRequestDTO {
    private DriverStatus status;
}
