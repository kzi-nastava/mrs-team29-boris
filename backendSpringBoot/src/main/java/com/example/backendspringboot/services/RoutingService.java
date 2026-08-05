package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.model.RoutePoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RoutingService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public RoutingService(ObjectMapper objectMapper,
                          @Value("${routing.osrm.base-url:https://router.project-osrm.org}")
                          String baseUrl) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public RoutingResult calculate(LocationDTO origin, List<LocationDTO> stops,
                                   LocationDTO destination) {
        List<LocationDTO> waypoints = new ArrayList<>();
        waypoints.add(origin);
        if (stops != null) waypoints.addAll(stops);
        waypoints.add(destination);

        String coordinates = waypoints.stream()
                .map(point -> String.format(Locale.US, "%.6f,%.6f",
                        point.getLongitude(), point.getLatitude()))
                .reduce((first, second) -> first + ";" + second)
                .orElseThrow();
        URI uri = URI.create(baseUrl + "/route/v1/driving/" + coordinates
                + "?overview=full&geometries=geojson&steps=false");
        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            return parseResponse(objectMapper.readTree(body));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Servis za računanje rute trenutno nije dostupan", exception);
        }
    }

    public List<Double> roadDistancesToDestination(List<LocationDTO> origins,
                                                   LocationDTO destination) {
        if (origins.isEmpty()) return List.of();
        List<LocationDTO> coordinates = new ArrayList<>(origins);
        coordinates.add(destination);
        String coordinatePath = coordinates.stream()
                .map(point -> String.format(Locale.US, "%.6f,%.6f",
                        point.getLongitude(), point.getLatitude()))
                .reduce((first, second) -> first + ";" + second)
                .orElseThrow();
        String sources = java.util.stream.IntStream.range(0, origins.size())
                .mapToObj(String::valueOf)
                .reduce((first, second) -> first + ";" + second)
                .orElseThrow();
        URI uri = URI.create(baseUrl + "/table/v1/driving/" + coordinatePath
                + "?annotations=distance&sources=" + sources
                + "&destinations=" + origins.size());
        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            return parseDistanceTable(objectMapper.readTree(body));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Servis za računanje udaljenosti vozila nije dostupan", exception);
        }
    }

    RoutingResult parseResponse(JsonNode root) {
        if (!"Ok".equals(root.path("code").asText()) || root.path("routes").isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nije pronađena putanja između izabranih tačaka");
        }
        JsonNode route = root.path("routes").get(0);
        List<RoutePoint> geometry = new ArrayList<>();
        for (JsonNode coordinate : route.path("geometry").path("coordinates")) {
            geometry.add(new RoutePoint(coordinate.get(0).asDouble(), coordinate.get(1).asDouble()));
        }
        if (geometry.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Routing servis nije vratio geometriju putanje");
        }
        double distanceKm = Math.round(route.path("distance").asDouble() / 10.0) / 100.0;
        int durationMinutes = Math.max(1,
                (int) Math.ceil(route.path("duration").asDouble() / 60.0));
        return new RoutingResult(distanceKm, durationMinutes, geometry);
    }

    List<Double> parseDistanceTable(JsonNode root) {
        if (!"Ok".equals(root.path("code").asText()) || !root.path("distances").isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Routing servis nije vratio drumske udaljenosti");
        }
        List<Double> distances = new ArrayList<>();
        for (JsonNode row : root.path("distances")) {
            JsonNode value = row.path(0);
            distances.add(value.isNumber() ? value.asDouble() / 1000.0 : Double.MAX_VALUE);
        }
        return distances;
    }
}
