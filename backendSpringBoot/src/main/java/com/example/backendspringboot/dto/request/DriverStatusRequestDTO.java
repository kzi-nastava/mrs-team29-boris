package com.example.demo.dto.request;

import com.example.demo.model.DriverStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverStatusRequestDTO {
    private DriverStatus status;
}
