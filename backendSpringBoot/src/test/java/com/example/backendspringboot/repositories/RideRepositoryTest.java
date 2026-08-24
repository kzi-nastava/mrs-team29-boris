package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan(basePackages = "com.example.backendspringboot.model")
public class RideRepositoryTest {
    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private TestEntityManager entityManager;

    /*
    * List<Ride> findAllByPassengerIdOrdered(@Param("passengerId") Long passengerId);*/

    @Test
    void findAllByStatus_shouldReturnRides() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.STARTED);

        rideRepository.save(ride);

        List<Ride> rides = rideRepository.findAllByStatus(RideStatus.STARTED);

        assertFalse(rides.isEmpty());
    }

    @Test
    void findAllByDriverId_shouldReturnRides() {

        Driver driver = new Driver();
        driver.setEmail("driver-repository-test@example.com");
        driver.setPassword("password");
        driver.setName("Test");
        driver.setSurname("Driver");
        driver.setGender(com.example.backendspringboot.model.Gender.MALE);
        driver.setAddress("Test address");
        driver.setPhone("000000000");
        driver.setStatus(com.example.backendspringboot.model.DriverStatus.ACTIVE);
        entityManager.persistAndFlush(driver);

        Ride ride = new Ride();
        ride.setDriver(driver);

        rideRepository.saveAndFlush(ride);

        List<Ride> rides = rideRepository.findAllByDriverId(driver.getId());

        assertFalse(rides.isEmpty());
    }

    @Test
    void findAllByPassengerId_shouldReturnRides() {

        Passenger creator = new Passenger();
        creator.setEmail("passenger-repository-test@example.com");
        creator.setPassword("password");
        creator.setName("Test");
        creator.setSurname("Passenger");
        creator.setGender(com.example.backendspringboot.model.Gender.FEMALE);
        creator.setAddress("Test address");
        creator.setPhone("000000001");
        entityManager.persistAndFlush(creator);
        Ride ride = new Ride();

        ride.setRideCreator(creator);

        rideRepository.saveAndFlush(ride);

        List<Ride> rides = rideRepository.findAllByPassengerId(creator.getId());

        assertFalse(rides.isEmpty());
    }

    @Test
    void existsRideParticipantRecognizesRideCreator() {
        Passenger creator = new Passenger();
        creator.setEmail("tracking-creator-repository-test@example.com");
        creator.setPassword("password");
        creator.setName("Tracking");
        creator.setSurname("Creator");
        creator.setGender(com.example.backendspringboot.model.Gender.MALE);
        creator.setAddress("Test address");
        creator.setPhone("000000002");
        entityManager.persistAndFlush(creator);

        Ride ride = new Ride();
        ride.setRideCreator(creator);
        ride = rideRepository.saveAndFlush(ride);

        assertTrue(rideRepository.existsRideParticipant(ride.getId(), creator.getId()));
    }
}
