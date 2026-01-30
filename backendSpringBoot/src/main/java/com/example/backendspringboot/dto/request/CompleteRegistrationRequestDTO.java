package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteRegistrationRequestDTO {

    @NotBlank(message = "Token cannot be blank")
    private String token;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must have at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9]).+$",
            message = "Password must contain at least one uppercase letter and one number"
    )
    private String password;

    @NotBlank(message = "Confirm password cannot be blank")
    private String confirmPassword;
}
