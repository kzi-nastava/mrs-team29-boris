package com.example.backendspringboot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_inconsistency_report_ride_passenger",
        columnNames = {"ride_id", "passenger_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InconsistencyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @ManyToOne
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    private String note;
    private LocalDateTime createdAt;
}
