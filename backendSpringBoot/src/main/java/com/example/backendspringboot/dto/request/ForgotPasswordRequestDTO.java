package com.example.backendspringboot.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class ForgotPasswordRequestDTO {
    @NotBlank
    @Email
    private String email;
}
