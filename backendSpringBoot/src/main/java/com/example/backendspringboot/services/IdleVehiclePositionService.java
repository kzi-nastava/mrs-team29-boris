package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.model.RoutePoint;
import com.example.backendspringboot.model.Vehicle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Service
public class IdleVehiclePositionService {
    private static final long ONE_WAY_SECONDS = 180L;
    private final RoutingService routingService;
    private final LongSupplier currentTimeMillis;
    private final Map<Long, IdleRoute> routes = new ConcurrentHashMap<>();

    @Autowired
    public IdleVehiclePositionService(RoutingService routingService) {
        this(routingService, System::currentTimeMillis);
    }

    IdleVehiclePositionService(RoutingService routingService, LongSupplier currentTimeMillis) {
        this.routingService = routingService;
        this.currentTimeMillis = currentTimeMillis;
    }

    public LocationDTO currentLocation(Vehicle vehicle) {
        if (vehicle == null || vehicle.getLocation() == null) return null;
        if (vehicle.getId() == null) return storedLocation(vehicle);
        IdleRoute route = routes.get(vehicle.getId());
        if (route == null || !route.matches(vehicle)) {
            try {
                route = createRoute(vehicle);
                routes.put(vehicle.getId(), route);
            } catch (ResponseStatusException exception) {
                return storedLocation(vehicle);
            }
        }
        long seconds = currentTimeMillis.getAsLong() / 1000L;
        long offset = Math.floorMod(seconds + vehicle.getId() * 37L,
                ONE_WAY_SECONDS * 2L);
        double progress = offset <= ONE_WAY_SECONDS
                ? offset / (double) ONE_WAY_SECONDS
                : (ONE_WAY_SECONDS * 2L - offset) / (double) ONE_WAY_SECONDS;
        RoutePoint point = pointAlong(route.geometry(), progress);
        return new LocationDTO(point.getLongitude(), point.getLatitude(),
                "Slobodno vozilo u pokretu");
    }

    private IdleRoute createRoute(Vehicle vehicle) {
        double angle = Math.toRadians((vehicle.getId() * 137.5) % 360.0);
        double latitudeOffset = 0.0055 * Math.sin(angle);
        double longitudeOffset = 0.0075 * Math.cos(angle);
        LocationDTO origin = storedLocation(vehicle);
        LocationDTO target = new LocationDTO(
                origin.getLongitude() + longitudeOffset,
                origin.getLatitude() + latitudeOffset,
                "Cilj kretanja slobodnog vozila");
        RoutingResult result = routingService.calculate(origin, List.of(), target);
        return new IdleRoute(origin.getLatitude(), origin.getLongitude(), result.geometry());
    }

    private static LocationDTO storedLocation(Vehicle vehicle) {
        return new LocationDTO(vehicle.getLocation().getLongitude(),
                vehicle.getLocation().getLatitude(), vehicle.getLocation().getAddress());
    }

    private static RoutePoint pointAlong(List<RoutePoint> points, double progress) {
        if (points.size() == 1) return points.get(0);
        double[] lengths = new double[points.size() - 1];
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            lengths[i - 1] = distance(points.get(i - 1), points.get(i));
            total += lengths[i - 1];
        }
        if (total == 0.0) return points.get(0);
        double target = total * progress;
        double covered = 0.0;
        for (int i = 0; i < lengths.length; i++) {
            if (covered + lengths[i] >= target) {
                double local = lengths[i] == 0.0 ? 0.0 : (target - covered) / lengths[i];
                RoutePoint from = points.get(i);
                RoutePoint to = points.get(i + 1);
                return new RoutePoint(
                        from.getLongitude() + (to.getLongitude() - from.getLongitude()) * local,
                        from.getLatitude() + (to.getLatitude() - from.getLatitude()) * local);
            }
            covered += lengths[i];
        }
        return points.get(points.size() - 1);
    }

    private static double distance(RoutePoint first, RoutePoint second) {
        double lat1 = Math.toRadians(first.getLatitude());
        double lat2 = Math.toRadians(second.getLatitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(second.getLongitude() - first.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    private record IdleRoute(double baseLatitude, double baseLongitude,
                             List<RoutePoint> geometry) {
        boolean matches(Vehicle vehicle) {
            return Double.compare(baseLatitude, vehicle.getLocation().getLatitude()) == 0
                    && Double.compare(baseLongitude, vehicle.getLocation().getLongitude()) == 0;
        }
    }
}
