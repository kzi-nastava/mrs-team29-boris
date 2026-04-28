package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.ProfileUpdateRequestDTO;
import com.example.backendspringboot.dto.request.ProfileVehicleUpdateRequestDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    @TempDir
    Path imageDirectory;
    private UserRepository userRepository;
    private VehicleRepository vehicleRepository;
    private DriverProfileChangeRequestRepository changeRepository;
    private ProfileService service;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        changeRepository = mock(DriverProfileChangeRequestRepository.class);
        service = new ProfileService(userRepository, vehicleRepository,
                mock(RideRepository.class), mock(GuestRideRepository.class),
                changeRepository, mock(PasswordEncoder.class));
        service.useImageDirectory(imageDirectory);
    }

    @Test
    void driverUpdateCreatesPendingRequestWithoutChangingVisibleProfile() {
        Driver driver = driver();
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(changeRepository.findFirstByDriverIdAndStatus(1L, ProfileChangeStatus.PENDING))
                .thenReturn(Optional.empty());

        service.updateOwnProfile(driver, updateRequest());

        assertEquals("Old", driver.getName());
        ArgumentCaptor<DriverProfileChangeRequest> captor =
                ArgumentCaptor.forClass(DriverProfileChangeRequest.class);
        verify(changeRepository).save(captor.capture());
        assertEquals("New", captor.getValue().getName());
        assertEquals(ProfileChangeStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    void administratorApprovalAppliesPendingDriverChanges() {
        Driver driver = driver();
        DriverProfileChangeRequest change = new DriverProfileChangeRequest();
        change.setId(5L);
        change.setDriver(driver);
        change.setStatus(ProfileChangeStatus.PENDING);
        change.setName("New");
        change.setSurname("Driver");
        change.setEmail("new@example.com");
        change.setGender(Gender.FEMALE);
        change.setAddress("New address");
        change.setPhone("0641234567");
        change.setVehicleModel("Tesla");
        change.setVehicleType(VehicleType.LUXURY);
        change.setVehicleRegistration("NS-NEW");
        change.setVehicleSeats(4);
        change.setBabyFriendly(true);
        change.setPetFriendly(false);
        when(changeRepository.findById(5L)).thenReturn(Optional.of(change));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(vehicleRepository.existsByRegistration("NS-NEW")).thenReturn(false);

        service.approve(5L);

        assertEquals("New", driver.getName());
        assertEquals("new@example.com", driver.getEmail());
        assertEquals("Tesla", driver.getVehicle().getModel());
        assertEquals(ProfileChangeStatus.APPROVED, change.getStatus());
    }

    @Test
    void activityDurationCountsOnlyPartInsideLast24Hours() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 12, 0);
        assertEquals(120, ProfileService.overlapMinutes(
                now.minusHours(26), now.minusHours(22), now.minusHours(24), now));
        assertEquals(30, ProfileService.overlapMinutes(
                now.minusMinutes(30), null, now.minusHours(24), now));
    }

    @Test
    void passengerImageIsStoredAndCanBeDeleted() throws Exception {
        Passenger passenger = new Passenger();
        passenger.setId(2L);
        passenger.setName("Ana");
        passenger.setSurname("Anić");
        passenger.setEmail("ana@example.com");
        passenger.setGender(Gender.FEMALE);
        passenger.setAddress("Adresa");
        passenger.setPhone("0641234567");
        when(userRepository.findById(2L)).thenReturn(Optional.of(passenger));
        MockMultipartFile image = new MockMultipartFile(
                "file", "photo.png", "image/png", new byte[]{1, 2, 3});

        String imageUrl = service.uploadOwnImage(passenger, image).getProfileImageUrl();
        Path stored = imageDirectory.resolve(imageUrl.substring(imageUrl.lastIndexOf('/') + 1));
        assertTrue(Files.exists(stored));
        assertEquals(imageUrl, passenger.getProfileImageUrl());

        service.deleteOwnImage(passenger);
        assertFalse(Files.exists(stored));
        assertNull(passenger.getProfileImageUrl());
    }

    private Driver driver() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(10L);
        vehicle.setModel("Old car");
        vehicle.setType(VehicleType.STANDARD);
        vehicle.setRegistration("NS-OLD");
        vehicle.setSeats(4);
        vehicle.setIsBabyFriendly(false);
        vehicle.setIsPetFriendly(false);
        Driver driver = new Driver();
        driver.setId(1L);
        driver.setName("Old");
        driver.setSurname("Driver");
        driver.setEmail("old@example.com");
        driver.setGender(Gender.MALE);
        driver.setAddress("Old address");
        driver.setPhone("0640000000");
        driver.setVehicle(vehicle);
        return driver;
    }

    private ProfileUpdateRequestDTO updateRequest() {
        ProfileVehicleUpdateRequestDTO vehicle = new ProfileVehicleUpdateRequestDTO();
        vehicle.setModel("Tesla");
        vehicle.setType(VehicleType.LUXURY);
        vehicle.setRegistration("NS-NEW");
        vehicle.setSeats(4);
        vehicle.setBabyFriendly(true);
        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setName("New");
        request.setSurname("Driver");
        request.setEmail("new@example.com");
        request.setGender(Gender.FEMALE);
        request.setAddress("New address");
        request.setPhone("0641234567");
        request.setVehicle(vehicle);
        return request;
    }
}
