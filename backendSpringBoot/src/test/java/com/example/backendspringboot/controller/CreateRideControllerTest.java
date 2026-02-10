package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.request.CreateRideRequestDTO;
import com.example.backendspringboot.dto.response.RideResponseDTO;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.model.VehicleType;
import com.example.backendspringboot.repositories.*;
import com.example.backendspringboot.services.interfaces.GuestRideService;
import com.example.backendspringboot.services.interfaces.ReviewService;
import com.example.backendspringboot.services.interfaces.RideService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(RideController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CreateRideControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RideService rideService;
    @MockitoBean
    private ReviewService reviewService;
    @MockitoBean
    private GuestRideService guestRideService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private RideRepository rideRepository;
    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private CreateRideRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        LocationDTO origin = new LocationDTO();
        origin.setLatitude(45.0);
        origin.setLongitude(19.0);
        origin.setAddress("Ulica A");

        LocationDTO destination = new LocationDTO();
        destination.setLatitude(46.0);
        destination.setLongitude(20.0);
        destination.setAddress("Ulica B");

        validRequest = new CreateRideRequestDTO();
        validRequest.setOrigin(origin);
        validRequest.setDestination(destination);
        validRequest.setPassengerId(1L);
        validRequest.setScheduledTime(LocalDateTime.now().plusMinutes(10));
        validRequest.setDurationMinutes(30);
        validRequest.setDistanceKm(5.0);
        validRequest.setBabyFriendly(false);
        validRequest.setPetFriendly(false);
        validRequest.setVehicleType(VehicleType.STANDARD);
    }

    @Test
    void whenValidRequest_thenReturns201() throws Exception {
        RideResponseDTO response = new RideResponseDTO(1L, RideStatus.SCHEDULED, 0.0);
        when(rideService.createRide(any())).thenReturn(response);

        mockMvc.perform(post("/api/rides/create-ride")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void whenServiceReturnsFailedStatus_thenReturns201WithFailedStatus() throws Exception {
        RideResponseDTO response = new RideResponseDTO(1L, RideStatus.FAILED, 0.0);
        when(rideService.createRide(any())).thenReturn(response);

        mockMvc.perform(post("/api/rides/create-ride")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void whenServiceThrowsException_thenReturns400() throws Exception {
        when(rideService.createRide(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger is blocked"));

        mockMvc.perform(post("/api/rides/create-ride")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
    }

    @Test
    void whenRequestBodyMissing_thenReturns400() throws Exception {
        mockMvc.perform(post("/api/rides/create-ride")
                        .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
    }
}
