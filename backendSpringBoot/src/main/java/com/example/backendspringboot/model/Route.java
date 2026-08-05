package com.example.backendspringboot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Location origin;

    @ManyToOne
    private Location destination;

    private int duration;

    private double distance;

    private int timesUsed;

    private int estimatedTime;

    @ElementCollection
    @CollectionTable(name = "route_geometry", joinColumns = @JoinColumn(name = "route_id"))
    @OrderColumn(name = "point_order")
    private List<RoutePoint> geometry = new ArrayList<>();
}
