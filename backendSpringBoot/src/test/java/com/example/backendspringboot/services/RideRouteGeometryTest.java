package com.example.backendspringboot.services;

import com.example.backendspringboot.model.Location;
import com.example.backendspringboot.model.Route;
import com.example.backendspringboot.model.RoutePoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RideRouteGeometryTest {

    @Test
    void trackingPositionFollowsStoredPolylineInsteadOfCuttingAcrossIt() {
        Route route = new Route();
        route.setOrigin(new Location(null, 0.0, 0.0, "origin"));
        route.setDestination(new Location(null, 1.0, 1.0, "destination"));
        route.setGeometry(List.of(
                new RoutePoint(0.0, 0.0),
                new RoutePoint(1.0, 0.0),
                new RoutePoint(1.0, 1.0)));

        RoutePoint middle = RideServiceImpl.pointAlongRoute(route, 0.5);

        assertEquals(1.0, middle.getLongitude(), 0.001);
        assertEquals(0.0, middle.getLatitude(), 0.001);
    }

    @Test
    void demoRideMovesBackAndForthInsteadOfStoppingAtDestination() {
        assertEquals(0.0, RideServiceImpl.loopingDemoProgress(0), 0.001);
        assertEquals(0.5, RideServiceImpl.loopingDemoProgress(45), 0.001);
        assertEquals(1.0, RideServiceImpl.loopingDemoProgress(90), 0.001);
        assertEquals(0.5, RideServiceImpl.loopingDemoProgress(135), 0.001);
        assertEquals(0.0, RideServiceImpl.loopingDemoProgress(180), 0.001);
        assertEquals(0.5, RideServiceImpl.loopingDemoProgress(225), 0.001);
    }
}
