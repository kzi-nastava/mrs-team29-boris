package com.example.backendspringboot.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinishRideRequestDTO {
    private long id;
    private double distance;
}
