package com.example.demo.dto.request;

import com.example.demo.dto.LocationDTO;
import com.example.demo.model.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateRideRequestDTO {

    @NotNull
    private Long passengerId; // which passenger created the ride

    @NotNull(message = "Origin cannot be null")
    @Valid
    private LocationDTO origin;

    @NotNull(message = "Destination cannot be null")
    @Valid
    private LocationDTO destination;

    @Valid
    private List<LocationDTO> stops;

    @Valid
    private List<@NotBlank @Email String> passengerEmails;

    @NotNull(message = "Vehicle type cannot be null")
    @Valid
    private VehicleType vehicleType;

    @NotNull(message = "Scheduled time cannot be null")
    @Future(message = "Scheduled time must be in the future")
    private LocalDateTime scheduledTime;

    private boolean babyFriendly;
    private boolean petFriendly;

    @NotNull(message = "Duration cannot be null")
    private int durationMinutes;

    @NotNull(message = "Distance cannot be null")
    private double distanceKm;
}
