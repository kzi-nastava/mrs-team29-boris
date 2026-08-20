package com.example.backendspringboot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private Passenger rideCreator;

    private LocalDateTime createdAt;

    // When saving an object into database, remember when created
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @ManyToOne(cascade = CascadeType.PERSIST)
    private Location currentLocation;

    @Enumerated(EnumType.STRING)
    private RideStatus status;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime scheduledTime;

    @ManyToOne
    @JoinColumn(name = "cancelled_by_id")
    private User cancelledBy;

    private String cancellationReason;

    @ManyToMany
    @JoinTable(
            name = "ride_passengers",
            joinColumns = @JoinColumn(name = "ride_id"),
            inverseJoinColumns = @JoinColumn(name = "passenger_id")
    )
    private List<Passenger> passengers;

    @ElementCollection
    @CollectionTable(name = "ride_linked_passenger_emails",
            joinColumns = @JoinColumn(name = "ride_id"))
    @Column(name = "email", nullable = false)
    private List<String> linkedPassengerEmails = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route;

    private double price;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleTypeAtBooking;
    private double basePriceAtBooking;
    private double pricePerKmAtBooking;
    private double distanceKm;

    private boolean isBabyFriendly;
    private boolean isPetFriendly;

    @ManyToMany
    @OrderColumn(name = "stop_order")
    private List<Location> stops;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL)
    private List<InconsistencyReport> inconsistencyReports;

    private boolean panicPressed;

    // Development seed rides use a repeating movement so occupied demo vehicles
    // remain visibly in motion for manual demonstrations.
    private boolean demoLoopingSimulation;

}
