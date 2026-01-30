package com.example.demo.dto.request;

import com.example.demo.model.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverRegistrationRequestDTO {

    @NotNull(message = "Email address cannot be null")
    @NotBlank(message = "Email address cannot be blank")
    @Email(message = "Invalid email format for driver registration")
    private String email;

    @NotNull(message = "Driver name cannot be null")
    @NotBlank(message = "Driver name cannot be blank")
    @Pattern(regexp = "^[A-Za-zČĆŽŠĐčćžšđ]+(?:[ -][A-Za-zČĆŽŠĐčćžšđ]+)*$")
    @Size(max = 25, message = "Driver name cannot be longer than 25 characters")
    private String name;

    @NotNull(message = "Driver last name cannot be null")
    @NotBlank(message = "Driver last name cannot be blank")
    @Pattern(regexp = "^[A-Za-zČĆŽŠĐčćžšđ]+(?:[ -][A-Za-zČĆŽŠĐčćžšđ]+)*$")
    @Size(max = 30, message = "Driver last name cannot be longer than 30 characters")
    private String surname;

    @NotNull(message = "Driver gender cannot be null")
    private Gender gender;

    @NotNull(message = "Driver address cannot be null")
    @NotBlank(message = "Driver address cannot be blank")
    @Size(max = 35, message = "Driver address cannot be longer than 35 characters")
    private String address;

    @NotNull(message = "Phone number cannot be null")
    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^(\\+381|0)?[6-7]\\d{7,8}$", message = "Phone must be of correct pattern")
    private String phone;

    @NotNull(message = "Driver vehicle cannot be null")
    @Valid
    private VehicleRegistrationRequestDTO vehicle;
}
