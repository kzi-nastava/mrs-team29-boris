package com.example.backendspringboot.controller;

import com.example.backendspringboot.model.Gender;
import com.example.backendspringboot.model.Location;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.model.Route;
import com.example.backendspringboot.repositories.LocationRepository;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.RouteRepository;
import com.example.backendspringboot.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RideTrackingEndpointIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired PassengerRepository passengerRepository;
    @Autowired LocationRepository locationRepository;
    @Autowired RouteRepository routeRepository;
    @Autowired RideRepository rideRepository;

    @Test
    void creatorCanTrackRideThroughJwtSecurityFilter() throws Exception {
        Passenger creator = new Passenger();
        creator.setEmail("tracking-endpoint@example.com");
        creator.setPassword("irrelevant-for-jwt-test");
        creator.setName("Tracking");
        creator.setSurname("Passenger");
        creator.setGender(Gender.MALE);
        creator.setAddress("Test address");
        creator.setPhone("000000099");
        creator.setActivated(true);
        creator = passengerRepository.saveAndFlush(creator);

        Location origin = locationRepository.saveAndFlush(
                new Location(null, 19.8335, 45.2671, "Polazište"));
        Location destination = locationRepository.saveAndFlush(
                new Location(null, 19.8420, 45.2550, "Odredište"));

        Route route = new Route();
        route.setOrigin(origin);
        route.setDestination(destination);
        route.setDuration(10);
        route = routeRepository.saveAndFlush(route);

        Ride ride = new Ride();
        ride.setRideCreator(creator);
        ride.setPassengers(new ArrayList<>());
        ride.setStops(new ArrayList<>());
        ride.setRoute(route);
        ride.setStatus(RideStatus.STARTED);
        ride.setStartTime(LocalDateTime.now());
        ride = rideRepository.saveAndFlush(ride);

        String token = jwtUtil.generateToken(creator);

        mockMvc.perform(get("/api/rides/{id}/tracking", ride.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(ride.getId()))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }
}
