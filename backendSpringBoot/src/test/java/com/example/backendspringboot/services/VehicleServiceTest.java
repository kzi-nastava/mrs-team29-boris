package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.response.ActiveVehicleResponseDTO;
import com.example.backendspringboot.dto.request.LoginRequestDTO;
import com.example.backendspringboot.model.Location;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.DriverStatus;
import com.example.backendspringboot.model.Vehicle;
import com.example.backendspringboot.repositories.VehicleRepository;
import com.example.backendspringboot.repositories.DriverRepository;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.UserRepository;
import com.example.backendspringboot.security.JwtUtil;
import com.example.backendspringboot.services.interfaces.EmailService;
import com.example.backendspringboot.services.interfaces.RideService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleServiceTest {

    @Test
    void returnsCoordinatesInLongitudeLatitudeOrderNearStoredLocation() {
        VehicleRepository repository = mock(VehicleRepository.class);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setBusy(false);
        vehicle.setLocation(new Location(
                1L,
                19.8302,
                45.2641,
                "Bulevar Kralja Petra I"
        ));
        DriverRepository drivers = mock(DriverRepository.class);
        Driver driver = driver(vehicle, DriverStatus.ACTIVE);
        when(drivers.findAllByStatus(DriverStatus.ACTIVE)).thenReturn(List.of(driver));
        IdleVehiclePositionService idlePositions = mock(IdleVehiclePositionService.class);
        when(idlePositions.currentLocation(vehicle)).thenReturn(
                new com.example.backendspringboot.dto.LocationDTO(
                        19.8302, 45.2641, "Bulevar Kralja Petra I"));

        ActiveVehicleResponseDTO result = new VehicleServiceImpl(repository,
                drivers, mock(RideService.class), idlePositions)
                .getAllActiveVehicles()
                .get(0);

        assertEquals(1L, result.getId());
        assertEquals("Marko Driverovic", result.getDriverName());
        assertFalse(result.isBusy());
        assertEquals("Bulevar Kralja Petra I", result.getCurrentLocation().getAddress());
        assertEquals(45.2641, result.getCurrentLocation().getLatitude());
        assertEquals(19.8302, result.getCurrentLocation().getLongitude());
    }

    @Test
    void doesNotSimulateIdleMovementForBusyVehicle() {
        VehicleRepository repository = mock(VehicleRepository.class);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setBusy(true);
        vehicle.setLocation(new Location(2L, 19.8820, 45.2426, "Futoški put"));
        DriverRepository drivers = mock(DriverRepository.class);
        Driver driver = driver(vehicle, DriverStatus.ACTIVE);
        when(drivers.findAllByStatus(DriverStatus.ACTIVE)).thenReturn(List.of(driver));

        ActiveVehicleResponseDTO result = new VehicleServiceImpl(repository,
                drivers, mock(RideService.class),
                mock(IdleVehiclePositionService.class))
                .getAllActiveVehicles()
                .get(0);

        assertEquals(19.8820, result.getCurrentLocation().getLongitude());
        assertEquals(45.2426, result.getCurrentLocation().getLatitude());
    }

    @Test
    void inactiveDriverVehicleIsNotShown() {
        VehicleRepository repository = mock(VehicleRepository.class);
        DriverRepository drivers = mock(DriverRepository.class);
        when(drivers.findAllByStatus(DriverStatus.ACTIVE)).thenReturn(List.of());

        List<ActiveVehicleResponseDTO> result = new VehicleServiceImpl(repository,
                drivers, mock(RideService.class), mock(IdleVehiclePositionService.class))
                .getAllActiveVehicles();

        assertEquals(0, result.size());
    }

    @Test
    void vehicleAppearsGreenAfterDriverLoginAndDisappearsAfterLogout() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(3L);
        vehicle.setBusy(false);
        vehicle.setLocation(new Location(3L, 19.8302, 45.2641,
                "Bulevar Kralja Petra I"));
        Driver driver = driver(vehicle, DriverStatus.INACTIVE);
        driver.setId(10L);
        driver.setEmail("driver@demo.com");
        driver.setPassword("encoded");

        DriverRepository drivers = mock(DriverRepository.class);
        when(drivers.findAllByStatus(DriverStatus.ACTIVE)).thenAnswer(invocation ->
                driver.getStatus() == DriverStatus.ACTIVE ? List.of(driver) : List.of());
        IdleVehiclePositionService idlePositions = mock(IdleVehiclePositionService.class);
        when(idlePositions.currentLocation(vehicle)).thenReturn(
                new com.example.backendspringboot.dto.LocationDTO(
                        19.8302, 45.2641, "Bulevar Kralja Petra I"));
        VehicleServiceImpl vehicles = new VehicleServiceImpl(mock(VehicleRepository.class),
                drivers, mock(RideService.class), idlePositions);

        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        JwtUtil jwt = mock(JwtUtil.class);
        when(users.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(passwords.matches("driver123", "encoded")).thenReturn(true);
        when(jwt.generateToken(driver)).thenReturn("jwt");
        UserServiceImpl usersService = new UserServiceImpl(passwords, users,
                mock(PassengerRepository.class), mock(EmailService.class), jwt,
                mock(VehicleRepository.class));
        LoginRequestDTO login = new LoginRequestDTO();
        login.setEmail(driver.getEmail());
        login.setPassword("driver123");

        assertEquals(0, vehicles.getAllActiveVehicles().size());
        usersService.login(login);
        List<ActiveVehicleResponseDTO> afterLogin = vehicles.getAllActiveVehicles();
        assertEquals(1, afterLogin.size());
        assertFalse(afterLogin.get(0).isBusy());
        usersService.logout(driver.getEmail());
        assertEquals(0, vehicles.getAllActiveVehicles().size());
    }

    private static Driver driver(Vehicle vehicle, DriverStatus status) {
        Driver driver = new Driver();
        driver.setName("Marko");
        driver.setSurname("Driverovic");
        driver.setStatus(status);
        driver.setVehicle(vehicle);
        return driver;
    }
}
