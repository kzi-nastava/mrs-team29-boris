package com.example.demo.dto.response;

import com.example.demo.model.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class InconsistencyReportResponseDTO {
    private Long id;
    private String note;
    private LocalDateTime createdAt;
    private String passengerEmail;

    //if necessary
    private Long rideId;
}