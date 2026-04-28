package com.example.backendspringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RidePassengerResponseDTO {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String phone;
    private String profileImageUrl;
}
