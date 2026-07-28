package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OwnProfileResponseDTO {
    private Long id;
    private String email;
    private String name;
    private String surname;
    private Gender gender;
    private String address;
    private String phone;
    private String profileImageUrl;
    private String role;
    private Integer activeMinutesLast24Hours;
    private ProfileVehicleResponseDTO vehicle;
    private boolean profileChangePending;
    private boolean blocked;
    private String blockReason;
}
