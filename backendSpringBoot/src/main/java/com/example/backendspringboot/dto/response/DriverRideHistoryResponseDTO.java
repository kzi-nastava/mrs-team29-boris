package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DriverRideHistoryResponseDTO {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocationDTO origin;
    private LocationDTO destination;
    private double totalPrice;
    private String vehicleTypeAtBooking;
    private double basePriceAtBooking;
    private double pricePerKmAtBooking;
    private double distanceKm;


    private List<InconsistencyReportResponseDTO> inconsistencyReports;

    private List<RidePassengerResponseDTO> passengers;
    private List<RideReviewResponseDTO> reviews;
    private boolean panicPressed;

    private String status;
    private boolean canceled;
    private String canceledBy;
    private String cancellationReason;
    private boolean guest;
}
