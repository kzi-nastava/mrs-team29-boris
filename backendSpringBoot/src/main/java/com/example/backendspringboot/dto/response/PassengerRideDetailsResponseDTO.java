package com.example.demo.dto.response;

import com.example.demo.dto.LocationDTO;
import com.example.demo.model.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassengerRideDetailsResponseDTO {
    private Long id;

    private String driverEmail;
    private String driverName;

    private Double totalPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String status;

    private LocationDTO origin;
    private LocationDTO destination;

    private int driverRating;
    private int vehicleRating;
    private List<String> inconsistencyReports;

    private boolean petFriendly;
    private boolean babyFriendly;
}
