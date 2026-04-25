package com.example.backendspringboot.dto.request;

import com.example.backendspringboot.model.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequestDTO {
    @NotBlank @Size(min = 2, max = 25)
    private String name;
    @NotBlank @Size(max = 30)
    private String surname;
    @NotBlank @Email
    private String email;
    @NotNull
    private Gender gender;
    @NotBlank @Size(max = 35)
    private String address;
    @NotBlank
    @Pattern(regexp = "^(\\+381|0)?[6-7]\\d{7,8}$")
    private String phone;
    @Valid
    private ProfileVehicleUpdateRequestDTO vehicle;
}
