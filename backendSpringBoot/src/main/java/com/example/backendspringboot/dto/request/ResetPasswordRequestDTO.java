package com.example.backendspringboot.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
public class ResetPasswordRequestDTO {
    @NotBlank
    private String token;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters and contain an uppercase letter and a number"
    )
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
