package com.example.demo.dto.response;

import com.example.demo.dto.LocationDTO;
import com.example.demo.model.RideStatus;
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
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocationDTO origin;
    private LocationDTO destination;
    private double totalPrice;


    private List<InconsistencyReportResponseDTO> inconsistencyReports;

    private List<String> passengerEmails;
    private boolean panicPressed;

    private String status;
}
