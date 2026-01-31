package com.example.backendspringboot.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideCancellationRequestDTO {
    private Long userId;
    private String reason;
    private boolean guest;
}
