package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserProfileRequestDTO {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 20, message = "Name cannot be more than 20 characters long")
    @Size(min = 2, message = "Name must have at least 2 characters")
    private String name;

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 30, message = "Last name cannot be more than 30 characters long")
    private String surname;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Provided email does not fit email format")
    private String email;

    @NotBlank(message = "Address cannot be blank")
    @Size(max = 35, message = "Address cannot be more than 35 characters long")
    private String address;

    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "^(\\+381|0)?[6-7]\\d{7,8}$", message = "Phone must be of correct pattern")
    private String phone;

    @Valid
    // Only if the user changing his information is a driver
    private UpdateVehicleRequestDTO vehicle;

    // private String password;
}
