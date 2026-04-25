package com.example.backendspringboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfilePasswordChangeRequestDTO {
    @NotBlank
    private String currentPassword;
    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$")
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}
