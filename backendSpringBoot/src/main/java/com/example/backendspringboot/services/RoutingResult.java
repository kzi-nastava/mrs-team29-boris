package com.example.backendspringboot.services;

import com.example.backendspringboot.model.RoutePoint;

import java.util.List;

public record RoutingResult(double distanceKm, int durationMinutes, List<RoutePoint> geometry) { }
