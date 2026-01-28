package com.example.backendspringboot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Panic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @ManyToOne
    @JoinColumn(name = "guest_ride_id")
    private GuestRide guestRide;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private boolean resolved = false;

    @ManyToOne
    @JoinColumn(name = "triggered_by_id")
    private User triggeredBy;
}
