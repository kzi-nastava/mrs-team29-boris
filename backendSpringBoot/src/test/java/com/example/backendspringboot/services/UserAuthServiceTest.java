package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.DriverStatusRequestDTO;
import com.example.backendspringboot.dto.request.LoginRequestDTO;
import com.example.backendspringboot.dto.request.ResetPasswordRequestDTO;
import com.example.backendspringboot.dto.response.DriverStatusResponseDTO;
import com.example.backendspringboot.dto.response.LoginResponseDTO;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.DriverStatus;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.UserRepository;
import com.example.backendspringboot.repositories.VehicleRepository;
import com.example.backendspringboot.security.JwtUtil;
import com.example.backendspringboot.services.interfaces.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAuthServiceTest {
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private JwtUtil jwtUtil;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = mock(PasswordEncoder.class);
        userRepository = mock(UserRepository.class);
        jwtUtil = mock(JwtUtil.class);
        service = new UserServiceImpl(
                passwordEncoder,
                userRepository,
                mock(PassengerRepository.class),
                mock(EmailService.class),
                jwtUtil,
                mock(VehicleRepository.class)
        );
    }

    @Test
    void driverBecomesActiveOnSuccessfulLogin() {
        Driver driver = driver(5L, DriverStatus.INACTIVE);
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(driver.getEmail());
        request.setPassword("Password1");
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(passwordEncoder.matches("Password1", driver.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(driver)).thenReturn("jwt");

        LoginResponseDTO response = service.login(request);

        assertEquals(DriverStatus.ACTIVE, driver.getStatus());
        assertEquals("driver", response.getRole());
        assertEquals("jwt", response.getToken());
        verify(userRepository).save(driver);
    }

    @Test
    void unknownEmailReturnsUnauthorizedWithoutRevealingWhetherAccountExists() {
        LoginRequestDTO request = loginRequest("missing@demo.com", "Password1");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid email or password", exception.getReason());
    }

    @Test
    void wrongPasswordReturnsSameUnauthorizedResponseAsUnknownEmail() {
        Driver driver = driver(6L, DriverStatus.INACTIVE);
        LoginRequestDTO request = loginRequest(driver.getEmail(), "WrongPassword1");
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(passwordEncoder.matches(request.getPassword(), driver.getPassword())).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid email or password", exception.getReason());
    }

    @Test
    void wrongPasswordDoesNotClaimThatPassengerNeedsActivation() {
        Passenger passenger = passenger(10L, false);
        LoginRequestDTO request = loginRequest(passenger.getEmail(), "WrongPassword1");
        when(userRepository.findByEmail(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(passwordEncoder.matches(request.getPassword(), passenger.getPassword()))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid email or password", exception.getReason());
    }

    @Test
    void correctPasswordForUnactivatedPassengerRequestsActivation() {
        Passenger passenger = passenger(11L, false);
        LoginRequestDTO request = loginRequest(passenger.getEmail(), "Password1");
        when(userRepository.findByEmail(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(passwordEncoder.matches(request.getPassword(), passenger.getPassword()))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.login(request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Account not activated. Check your email.", exception.getReason());
    }

    @Test
    void inactiveRequestDuringRideIsDeferred() {
        Driver driver = driver(7L, DriverStatus.ACTIVE);
        driver.setActiveRide(new Ride());
        DriverStatusRequestDTO request = new DriverStatusRequestDTO();
        request.setStatus(DriverStatus.INACTIVE);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(userRepository.findById(driver.getId())).thenReturn(Optional.of(driver));

        DriverStatusResponseDTO response = service.changeDriverStatus(
                driver.getId(), request, driver.getEmail());

        assertEquals(DriverStatus.ACTIVE, response.getStatus());
        assertTrue(response.isDeactivateAfterRide());
        assertTrue(response.isActiveRide());
    }

    @Test
    void driverCannotLogoutDuringActiveRide() {
        Driver driver = driver(8L, DriverStatus.ACTIVE);
        driver.setActiveRide(new Ride());
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.logout(driver.getEmail())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void passwordResetChangesPasswordAndInvalidatesToken() {
        Driver driver = driver(9L, DriverStatus.INACTIVE);
        driver.setPasswordResetToken("reset-token");
        driver.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setToken("reset-token");
        request.setNewPassword("NewPassword1");
        request.setConfirmPassword("NewPassword1");
        when(userRepository.findByPasswordResetToken("reset-token"))
                .thenReturn(Optional.of(driver));
        when(passwordEncoder.encode("NewPassword1")).thenReturn("encoded");

        service.resetPassword(request);

        assertEquals("encoded", driver.getPassword());
        assertNull(driver.getPasswordResetToken());
        assertNull(driver.getPasswordResetTokenExpiry());
        verify(userRepository).save(driver);
    }

    private Driver driver(long id, DriverStatus status) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setEmail("driver" + id + "@demo.com");
        driver.setPassword("encoded-old-password");
        driver.setStatus(status);
        driver.setDeactivateAfterRide(false);
        return driver;
    }

    private Passenger passenger(long id, boolean activated) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setEmail("passenger" + id + "@demo.com");
        passenger.setPassword("encoded-old-password");
        passenger.setActivated(activated);
        return passenger;
    }

    private LoginRequestDTO loginRequest(String email, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
