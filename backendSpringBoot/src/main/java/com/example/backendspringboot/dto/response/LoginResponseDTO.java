package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.model.Gender;
import com.example.backendspringboot.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private Long userId;
    private String email;
    private String role;
    private String token;

    private boolean isBlocked;
    private String blockReason;
}