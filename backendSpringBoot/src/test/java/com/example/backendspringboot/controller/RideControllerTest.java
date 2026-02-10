package com.example.backendspringboot.controller;

import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.LocationRepository;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.RouteRepository;
import com.example.backendspringboot.repositories.UserRepository;
import com.example.backendspringboot.repositories.VehicleRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RouteRepository routeRepository;

    private Ride rideStarted;
    private Ride rideScheduled;

    @BeforeEach
    void setup() {

        Location vehicleLocation = new Location();
        vehicleLocation.setLatitude(45.0);
        vehicleLocation.setLongitude(19.0);
        vehicleLocation.setAddress("Garage");
        vehicleLocation = locationRepository.save(vehicleLocation);

        Vehicle vehicle = new Vehicle();
        vehicle.setModel("Test Model");
        vehicle.setRegistration("REG123");
        vehicle.setType(VehicleType.STANDARD);
        vehicle.setLocation(vehicleLocation);
        vehicle = vehicleRepository.save(vehicle);

        Driver driver = new Driver();
        driver.setStatus(DriverStatus.ACTIVE);
        driver.setVehicle(vehicle);

        driver.setEmail("driver@example.com");
        driver.setPassword("password");
        driver.setName("John");
        driver.setSurname("Doe");
        driver.setGender(Gender.MALE);
        driver.setAddress("Some address");
        driver.setPhone("+381600000000");
//        driver.setChat(new Chat());

        driver = userRepository.save(driver);

        Location startLocation = new Location();
        startLocation.setLatitude(45.1);
        startLocation.setLongitude(19.1);
        startLocation.setAddress("Start");
        startLocation = locationRepository.save(startLocation);

        Location endLocation = new Location();
        endLocation.setLatitude(45.2);
        endLocation.setLongitude(19.2);
        endLocation.setAddress("End");
        endLocation = locationRepository.save(endLocation);

        Route route = new Route();
        route.setOrigin(startLocation);
        route.setDestination(endLocation);
        route = routeRepository.save(route);

        rideStarted = new Ride();
        rideStarted.setStatus(RideStatus.STARTED);
        rideStarted.setDriver(driver);
        rideStarted.setRoute(route);
        rideStarted = rideRepository.save(rideStarted);

        rideScheduled = new Ride();
        rideScheduled.setStatus(RideStatus.SCHEDULED);
        rideScheduled.setDriver(driver);
        rideScheduled.setRoute(route);
        rideScheduled = rideRepository.save(rideScheduled);
    }

    @Test
    void stopRide_success() throws Exception {
        String json = """
        {
            "guest": false,
            "stopLocation": {
                "latitude": 45.0,
                "longitude": 19.0,
                "address": "Test Address"
            }
        }
        """;

        mockMvc.perform(post("/api/rides/" + rideStarted.getId() + "/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void stopRide_badRequest_whenMissingLocation() throws Exception {
        String json = """
        {
            "guest": false
        }
        """;

        mockMvc.perform(post("/api/rides/" + rideStarted.getId() + "/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stopRide_badRequest_whenRideNotStarted() throws Exception {
        String json = """
        {
            "guest": false,
            "stopLocation": {
                "latitude": 45.0,
                "longitude": 19.0,
                "address": "Test Address"
            }
        }
        """;

        mockMvc.perform(post("/api/rides/" + rideScheduled.getId() + "/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
