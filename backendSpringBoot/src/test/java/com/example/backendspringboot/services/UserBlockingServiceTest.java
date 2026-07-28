package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.DriverStatusRequestDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.UserRepository;
import com.example.backendspringboot.repositories.VehicleRepository;
import com.example.backendspringboot.security.JwtUtil;
import com.example.backendspringboot.services.interfaces.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserBlockingServiceTest {
    private UserRepository userRepository;
    private UserServiceImpl service;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new UserServiceImpl(mock(PasswordEncoder.class), userRepository,
                mock(PassengerRepository.class), mock(EmailService.class),
                mock(JwtUtil.class), mock(VehicleRepository.class));
        ReflectionTestUtils.setField(service, "messagingTemplate", messagingTemplate);
    }

    @Test
    void blockingIdleDriverMakesDriverInactiveAndStoresTrimmedNote() {
        Driver driver = driver(5L, DriverStatus.ACTIVE);
        when(userRepository.findById(5L)).thenReturn(Optional.of(driver));

        UserProfileResponseDTO result = service.blockUser(5L, "  Prekršaj pravila  ");

        assertTrue(result.isBlocked());
        assertEquals("Prekršaj pravila", result.getBlockReason());
        assertEquals(DriverStatus.INACTIVE, driver.getStatus());
        assertFalse(driver.isDeactivateAfterRide());
        verify(userRepository).save(driver);
    }

    @Test
    void blockingDriverDuringRideDefersDeactivation() {
        Driver driver = driver(6L, DriverStatus.ACTIVE);
        driver.setActiveRide(new Ride());
        when(userRepository.findById(6L)).thenReturn(Optional.of(driver));

        service.blockUser(6L, "Napomena");

        assertEquals(DriverStatus.ACTIVE, driver.getStatus());
        assertTrue(driver.isDeactivateAfterRide());
    }

    @Test
    void blockedDriverCannotManuallyBecomeActive() {
        Driver driver = driver(7L, DriverStatus.INACTIVE);
        driver.setBlocked(true);
        DriverStatusRequestDTO request = new DriverStatusRequestDTO();
        request.setStatus(DriverStatus.ACTIVE);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(userRepository.findById(7L)).thenReturn(Optional.of(driver));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.changeDriverStatus(7L, request, driver.getEmail()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(DriverStatus.INACTIVE, driver.getStatus());
    }

    @Test
    void unblockClearsReasonButDoesNotActivateDriverAutomatically() {
        Driver driver = driver(8L, DriverStatus.INACTIVE);
        driver.setBlocked(true);
        driver.setBlockReason("Razlog");
        when(userRepository.findById(8L)).thenReturn(Optional.of(driver));

        UserProfileResponseDTO result = service.unblockUser(8L);

        assertFalse(result.isBlocked());
        assertNull(result.getBlockReason());
        assertEquals(DriverStatus.INACTIVE, driver.getStatus());
    }

    @Test
    void administratorCannotBeBlocked() {
        Administrator administrator = new Administrator();
        administrator.setId(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(administrator));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.blockUser(9L, "Nije dozvoljeno"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    private Driver driver(long id, DriverStatus status) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setEmail("driver" + id + "@demo.com");
        driver.setStatus(status);
        return driver;
    }
}
