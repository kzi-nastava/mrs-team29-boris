package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
public class DriverRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DriverRepository driverRepository;

    private Driver createDriver(DriverStatus status, boolean baby, boolean pet, VehicleType type) {
        Vehicle vehicle = new Vehicle();
        vehicle.setModel("Toyota Prius");
        vehicle.setType(type);
        vehicle.setRegistration("NS-" + System.nanoTime());
        vehicle.setSeats(4);
        vehicle.setIsBabyFriendly(baby);
        vehicle.setIsPetFriendly(pet);
        vehicle.setBusy(false);
        entityManager.persist(vehicle);

        Driver driver = new Driver();
        driver.setEmail("driver" + System.nanoTime() + "@test.com");
        driver.setPassword("password123");
        driver.setName("Lazar");
        driver.setSurname("Topic");
        driver.setGender(Gender.MALE);
        driver.setAddress("Nikole Tesle");
        driver.setPhone("069669949");
        driver.setBlocked(false);
        driver.setStatus(status);
        driver.setVehicle(vehicle);
        entityManager.persist(driver);

        return driver;
    }

    @Test
    public void whenAllFiltersMatch_thenReturnDriver() {
        createDriver(DriverStatus.ACTIVE, true, true, VehicleType.STANDARD);

        List<Driver> result = driverRepository.filterAvailableDrivers(DriverStatus.ACTIVE, true, true, VehicleType.STANDARD);

        assertEquals(1, result.size());
    }

    @Test
    public void whenDriverIsInactive_thenReturnEmpty() {
        createDriver(DriverStatus.INACTIVE, true, true, VehicleType.STANDARD);

        List<Driver> result = driverRepository.filterAvailableDrivers(DriverStatus.ACTIVE, true, true, VehicleType.STANDARD);

        assertTrue(result.isEmpty());
    }

    @Test
    public void whenBabyFriendlyDoesNotMatch_thenReturnEmpty() {
        createDriver(DriverStatus.ACTIVE, false, true, VehicleType.STANDARD);

        List<Driver> result = driverRepository.filterAvailableDrivers(DriverStatus.ACTIVE, true, true, VehicleType.STANDARD);

        assertTrue(result.isEmpty());
    }

    @Test
    public void whenPetFriendlyDoesNotMatch_thenReturnEmpty() {
        createDriver(DriverStatus.ACTIVE, true, false, VehicleType.STANDARD);

        List<Driver> result = driverRepository.filterAvailableDrivers(
                DriverStatus.ACTIVE, true, true, VehicleType.STANDARD);

        assertTrue(result.isEmpty());
    }

    @Test
    public void whenVehicleTypeDoesNotMatch_thenReturnEmpty() {
        createDriver(DriverStatus.ACTIVE, true, true, VehicleType.VAN);

        List<Driver> result = driverRepository.filterAvailableDrivers(DriverStatus.ACTIVE, true, true, VehicleType.STANDARD);

        assertTrue(result.isEmpty());
    }

    @Test
    public void whenMultipleDrivers_thenReturnOnlyMatching() {
        createDriver(DriverStatus.ACTIVE, true, true, VehicleType.STANDARD);
        createDriver(DriverStatus.ACTIVE, false, true, VehicleType.STANDARD);
        createDriver(DriverStatus.INACTIVE, true, true, VehicleType.STANDARD);

        List<Driver> result = driverRepository.filterAvailableDrivers(DriverStatus.ACTIVE, true, true, VehicleType.STANDARD);

        assertEquals(1, result.size());
    }
}
