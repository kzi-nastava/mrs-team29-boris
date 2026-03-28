package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.DriverStatusRequestDTO;
import com.example.backendspringboot.dto.request.LoginRequestDTO;
import com.example.backendspringboot.dto.request.ResetPasswordRequestDTO;
import com.example.backendspringboot.dto.response.DriverStatusResponseDTO;
import com.example.backendspringboot.dto.response.LoginResponseDTO;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.DriverStatus;
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
}
