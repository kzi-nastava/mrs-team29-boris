package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.model.Location;
import com.example.backendspringboot.model.RoutePoint;
import com.example.backendspringboot.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdleVehiclePositionServiceTest {

    @Test
    void freeVehicleMovesAlongCachedRoadGeometry() {
        RoutingService routingService = mock(RoutingService.class);
        when(routingService.calculate(any(), any(), any())).thenReturn(
                new RoutingResult(2.0, 4, List.of(
                        new RoutePoint(19.80, 45.25),
                        new RoutePoint(19.82, 45.25))));
        AtomicLong time = new AtomicLong(0L);
        IdleVehiclePositionService service =
                new IdleVehiclePositionService(routingService, time::get);
        Vehicle vehicle = vehicle(0L);

        LocationDTO start = service.currentLocation(vehicle);
        time.set(90_000L);
        LocationDTO moved = service.currentLocation(vehicle);

        assertEquals(19.80, start.getLongitude(), 0.0001);
        assertNotEquals(start.getLongitude(), moved.getLongitude());
        assertEquals(45.25, moved.getLatitude(), 0.0001);
        verify(routingService).calculate(any(), any(), any());
    }

    @Test
    void secondHalfOfCycleReturnsVehicleAlongSameRoadRoute() {
        RoutingService routingService = mock(RoutingService.class);
        when(routingService.calculate(any(), any(), any())).thenReturn(
                new RoutingResult(1.0, 2, List.of(
                        new RoutePoint(19.80, 45.25), new RoutePoint(19.82, 45.25))));
        AtomicLong time = new AtomicLong(90_000L);
        IdleVehiclePositionService service =
                new IdleVehiclePositionService(routingService, time::get);
        Vehicle vehicle = vehicle(0L);

        double outboundLongitude = service.currentLocation(vehicle).getLongitude();
        time.set(270_000L);
        double returningLongitude = service.currentLocation(vehicle).getLongitude();

        assertEquals(outboundLongitude, returningLongitude, 0.0001);
    }

    private Vehicle vehicle(long id) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setBusy(false);
        vehicle.setLocation(new Location(1L, 19.80, 45.25, "Početna lokacija"));
        return vehicle;
    }
}
