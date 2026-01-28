package com.example.backendspringboot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@DiscriminatorValue("PASSENGER")
public class Passenger extends User {

    @ManyToMany
    @JoinTable(
            name = "passenger_favorite_routes",
            joinColumns = @JoinColumn(name = "passenger_id"),
            inverseJoinColumns = @JoinColumn(name = "route_id")
    )

    private List<Route> favoriteRoutes;

    @Column(nullable = false)
    private boolean activated = false;

    @Column(unique = true)
    private String activationToken;

    @Column
    private LocalDateTime activationTokenExpiry;
}