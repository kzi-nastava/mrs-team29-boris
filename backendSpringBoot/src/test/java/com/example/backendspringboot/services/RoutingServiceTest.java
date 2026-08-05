package com.example.backendspringboot.services;

import com.example.backendspringboot.model.RoutePoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutingServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RoutingService service = new RoutingService(objectMapper, "http://routing.test");

    @Test
    void parsesRoadDistanceDurationAndOrderedGeoJsonCoordinates() throws Exception {
        String response = """
                {"code":"Ok","routes":[{"distance":5321.4,"duration":721.1,
                "geometry":{"type":"LineString","coordinates":[
                [19.8001,45.2501],[19.8102,45.2602],[19.8203,45.2703]]}}]}
                """;

        RoutingResult result = service.parseResponse(objectMapper.readTree(response));

        assertEquals(5.32, result.distanceKm(), 0.001);
        assertEquals(13, result.durationMinutes());
        assertEquals(3, result.geometry().size());
        RoutePoint middle = result.geometry().get(1);
        assertEquals(19.8102, middle.getLongitude(), 0.000001);
        assertEquals(45.2602, middle.getLatitude(), 0.000001);
    }

    @Test
    void rejectsNoRouteResponse() throws Exception {
        assertThrows(ResponseStatusException.class, () -> service.parseResponse(
                objectMapper.readTree("{\"code\":\"NoRoute\",\"routes\":[]}")));
    }

    @Test
    void parsesRoadDistanceTableInSourceOrder() throws Exception {
        String response = "{\"code\":\"Ok\",\"distances\":[[1250.0],[830.5],[null]]}";

        java.util.List<Double> distances = service.parseDistanceTable(
                objectMapper.readTree(response));

        assertEquals(1.25, distances.get(0), 0.001);
        assertEquals(0.8305, distances.get(1), 0.001);
        assertEquals(Double.MAX_VALUE, distances.get(2));
    }
}
