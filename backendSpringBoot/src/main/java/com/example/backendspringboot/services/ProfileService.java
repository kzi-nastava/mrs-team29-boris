package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.ProfilePasswordChangeRequestDTO;
import com.example.backendspringboot.dto.request.ProfileUpdateRequestDTO;
import com.example.backendspringboot.dto.request.ProfileVehicleUpdateRequestDTO;
import com.example.backendspringboot.dto.response.*;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    private final GuestRideRepository guestRideRepository;
    private final DriverProfileChangeRequestRepository changeRequestRepository;
    private final PasswordEncoder passwordEncoder;

    private final Path imageDirectory = Paths.get("uploads/profile-images");

    @PostConstruct
    void initializeStorage() throws IOException {
        Files.createDirectories(imageDirectory);
    }

    public OwnProfileResponseDTO getOwnProfile(User authenticatedUser) {
        User user = requireCurrentUser(authenticatedUser);
        return mapProfile(user, hasPendingChange(user));
    }

    @Transactional
    public OwnProfileResponseDTO updateOwnProfile(User authenticatedUser,
                                                   ProfileUpdateRequestDTO request) {
        User user = requireCurrentUser(authenticatedUser);
        validateUniqueValues(user, request);
        if (user instanceof Driver driver) {
            DriverProfileChangeRequest change = pendingChange(driver);
            copyRequest(change, request);
            change.setCreatedAt(LocalDateTime.now());
            changeRequestRepository.save(change);
            return mapProfile(user, true);
        }

        applyUserFields(user, request);
        userRepository.save(user);
        return mapProfile(user, false);
    }

    @Transactional
    public OwnProfileResponseDTO uploadOwnImage(User authenticatedUser, MultipartFile file) {
        User user = requireCurrentUser(authenticatedUser);
        String imageUrl = storeImage(user.getId(), file);
        if (user instanceof Driver driver) {
            DriverProfileChangeRequest change = pendingChange(driver);
            deletePendingImage(change);
            change.setProfileImageUrl(imageUrl);
            change.setRemoveProfileImage(false);
            change.setCreatedAt(LocalDateTime.now());
            changeRequestRepository.save(change);
            return mapProfile(user, true);
        }
        deleteImage(user.getProfileImageUrl());
        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);
        return mapProfile(user, false);
    }

    @Transactional
    public OwnProfileResponseDTO deleteOwnImage(User authenticatedUser) {
        User user = requireCurrentUser(authenticatedUser);
        if (user instanceof Driver driver) {
            DriverProfileChangeRequest change = pendingChange(driver);
            deletePendingImage(change);
            change.setProfileImageUrl(null);
            change.setRemoveProfileImage(true);
            change.setCreatedAt(LocalDateTime.now());
            changeRequestRepository.save(change);
            return mapProfile(user, true);
        }
        deleteImage(user.getProfileImageUrl());
        user.setProfileImageUrl(null);
        userRepository.save(user);
        return mapProfile(user, false);
    }

    @Transactional
    public void changePassword(User authenticatedUser, ProfilePasswordChangeRequestDTO request) {
        User user = requireCurrentUser(authenticatedUser);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New passwords do not match");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public List<DriverProfileChangeResponseDTO> pendingDriverChanges() {
        return changeRequestRepository
                .findAllByStatusOrderByCreatedAtAsc(ProfileChangeStatus.PENDING)
                .stream().map(this::mapChange).toList();
    }

    @Transactional
    public void approve(Long requestId) {
        DriverProfileChangeRequest change = requirePendingRequest(requestId);
        Driver driver = change.getDriver();
        ensureApprovalStillUnique(change);

        driver.setName(change.getName());
        driver.setSurname(change.getSurname());
        driver.setEmail(change.getEmail());
        driver.setGender(change.getGender());
        driver.setAddress(change.getAddress());
        driver.setPhone(change.getPhone());
        if (change.isRemoveProfileImage()) {
            deleteImage(driver.getProfileImageUrl());
            driver.setProfileImageUrl(null);
        } else if (change.getProfileImageUrl() != null) {
            deleteImage(driver.getProfileImageUrl());
            driver.setProfileImageUrl(change.getProfileImageUrl());
        }

        Vehicle vehicle = driver.getVehicle();
        vehicle.setModel(change.getVehicleModel());
        vehicle.setType(change.getVehicleType());
        vehicle.setRegistration(change.getVehicleRegistration());
        vehicle.setSeats(change.getVehicleSeats());
        vehicle.setIsBabyFriendly(change.getBabyFriendly());
        vehicle.setIsPetFriendly(change.getPetFriendly());
        vehicleRepository.save(vehicle);
        userRepository.save(driver);
        change.setStatus(ProfileChangeStatus.APPROVED);
        changeRequestRepository.save(change);
    }

    @Transactional
    public void reject(Long requestId) {
        DriverProfileChangeRequest change = requirePendingRequest(requestId);
        deletePendingImage(change);
        change.setStatus(ProfileChangeStatus.REJECTED);
        changeRequestRepository.save(change);
    }

    private User requireCurrentUser(User principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private boolean hasPendingChange(User user) {
        return user instanceof Driver && changeRequestRepository
                .findFirstByDriverIdAndStatus(user.getId(), ProfileChangeStatus.PENDING)
                .isPresent();
    }

    private DriverProfileChangeRequest pendingChange(Driver driver) {
        return changeRequestRepository
                .findFirstByDriverIdAndStatus(driver.getId(), ProfileChangeStatus.PENDING)
                .orElseGet(() -> initializedChange(driver));
    }

    private DriverProfileChangeRequest initializedChange(Driver driver) {
        DriverProfileChangeRequest change = new DriverProfileChangeRequest();
        change.setDriver(driver);
        change.setStatus(ProfileChangeStatus.PENDING);
        change.setCreatedAt(LocalDateTime.now());
        change.setName(driver.getName());
        change.setSurname(driver.getSurname());
        change.setEmail(driver.getEmail());
        change.setGender(driver.getGender());
        change.setAddress(driver.getAddress());
        change.setPhone(driver.getPhone());
        Vehicle vehicle = driver.getVehicle();
        change.setVehicleModel(vehicle.getModel());
        change.setVehicleType(vehicle.getType());
        change.setVehicleRegistration(vehicle.getRegistration());
        change.setVehicleSeats(vehicle.getSeats());
        change.setBabyFriendly(Boolean.TRUE.equals(vehicle.getIsBabyFriendly()));
        change.setPetFriendly(Boolean.TRUE.equals(vehicle.getIsPetFriendly()));
        return change;
    }

    private void copyRequest(DriverProfileChangeRequest change, ProfileUpdateRequestDTO request) {
        change.setName(request.getName());
        change.setSurname(request.getSurname());
        change.setEmail(request.getEmail());
        change.setGender(request.getGender());
        change.setAddress(request.getAddress());
        change.setPhone(request.getPhone());
        ProfileVehicleUpdateRequestDTO vehicle = request.getVehicle();
        if (vehicle != null) {
            change.setVehicleModel(vehicle.getModel());
            change.setVehicleType(vehicle.getType());
            change.setVehicleRegistration(vehicle.getRegistration());
            change.setVehicleSeats(vehicle.getSeats());
            change.setBabyFriendly(vehicle.isBabyFriendly());
            change.setPetFriendly(vehicle.isPetFriendly());
        }
    }

    private void applyUserFields(User user, ProfileUpdateRequestDTO request) {
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone());
    }

    private void validateUniqueValues(User user, ProfileUpdateRequestDTO request) {
        userRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> { throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Email already exists"); });
        if (user instanceof Driver driver && request.getVehicle() != null
                && !driver.getVehicle().getRegistration()
                .equalsIgnoreCase(request.getVehicle().getRegistration())
                && vehicleRepository.existsByRegistration(request.getVehicle().getRegistration())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vehicle registration already exists");
        }
    }

    private void ensureApprovalStillUnique(DriverProfileChangeRequest change) {
        userRepository.findByEmail(change.getEmail())
                .filter(existing -> !existing.getId().equals(change.getDriver().getId()))
                .ifPresent(existing -> { throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Email already exists"); });
        Vehicle current = change.getDriver().getVehicle();
        if (!current.getRegistration().equalsIgnoreCase(change.getVehicleRegistration())
                && vehicleRepository.existsByRegistration(change.getVehicleRegistration())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vehicle registration already exists");
        }
    }

    private DriverProfileChangeRequest requirePendingRequest(Long id) {
        DriverProfileChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (change.getStatus() != ProfileChangeStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Profile change request is already resolved");
        }
        return change;
    }

    private OwnProfileResponseDTO mapProfile(User user, boolean pending) {
        String role = user instanceof Administrator ? "admin"
                : user instanceof Driver ? "driver" : "user";
        ProfileVehicleResponseDTO vehicle = null;
        Integer activeMinutes = null;
        if (user instanceof Driver driver) {
            Vehicle v = driver.getVehicle();
            vehicle = new ProfileVehicleResponseDTO(v.getId(), v.getModel(), v.getType(),
                    v.getRegistration(), v.getSeats(), Boolean.TRUE.equals(v.getIsBabyFriendly()),
                    Boolean.TRUE.equals(v.getIsPetFriendly()));
            activeMinutes = activeMinutesLast24Hours(driver);
        }
        return new OwnProfileResponseDTO(user.getId(), user.getEmail(), user.getName(),
                user.getSurname(), user.getGender(), user.getAddress(), user.getPhone(),
                user.getProfileImageUrl(), role, activeMinutes, vehicle, pending);
    }

    private DriverProfileChangeResponseDTO mapChange(DriverProfileChangeRequest change) {
        Driver driver = change.getDriver();
        ProfileVehicleResponseDTO vehicle = new ProfileVehicleResponseDTO(
                driver.getVehicle().getId(), change.getVehicleModel(), change.getVehicleType(),
                change.getVehicleRegistration(), change.getVehicleSeats(),
                Boolean.TRUE.equals(change.getBabyFriendly()),
                Boolean.TRUE.equals(change.getPetFriendly()));
        String image = change.isRemoveProfileImage() ? null
                : change.getProfileImageUrl() != null ? change.getProfileImageUrl()
                : driver.getProfileImageUrl();
        OwnProfileResponseDTO proposed = new OwnProfileResponseDTO(driver.getId(),
                change.getEmail(), change.getName(), change.getSurname(), change.getGender(),
                change.getAddress(), change.getPhone(), image, "driver",
                activeMinutesLast24Hours(driver), vehicle, true);
        return new DriverProfileChangeResponseDTO(change.getId(), driver.getId(),
                driver.getEmail(), change.getCreatedAt(), proposed);
    }

    private int activeMinutesLast24Hours(Driver driver) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(24);
        int minutes = 0;
        for (Ride ride : rideRepository.findAllByDriverId(driver.getId())) {
            minutes += overlapMinutes(ride.getStartTime(), ride.getEndTime(), cutoff, now);
        }
        for (GuestRide ride : guestRideRepository.findAllByDriverId(driver.getId())) {
            minutes += overlapMinutes(ride.getStartTime(), ride.getEndTime(), cutoff, now);
        }
        return minutes;
    }

    static int overlapMinutes(LocalDateTime start, LocalDateTime end,
                              LocalDateTime cutoff, LocalDateTime now) {
        if (start == null) return 0;
        LocalDateTime effectiveStart = start.isBefore(cutoff) ? cutoff : start;
        LocalDateTime effectiveEnd = end == null || end.isAfter(now) ? now : end;
        return effectiveEnd.isAfter(effectiveStart)
                ? (int) Duration.between(effectiveStart, effectiveEnd).toMinutes() : 0;
    }

    private String storeImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null
                || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid image is required");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is larger than 5 MB");
        }
        String extension = file.getContentType().contains("png") ? ".png"
                : file.getContentType().contains("webp") ? ".webp" : ".jpg";
        String filename = userId + "_" + System.currentTimeMillis() + extension;
        try {
            Files.copy(file.getInputStream(), imageDirectory.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            return "/api/user/profile-images/" + filename;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store profile image");
        }
    }

    private void deletePendingImage(DriverProfileChangeRequest change) {
        if (change.getProfileImageUrl() != null
                && !change.getProfileImageUrl().equals(change.getDriver().getProfileImageUrl())) {
            deleteImage(change.getProfileImageUrl());
        }
    }

    private void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Files.deleteIfExists(imageDirectory.resolve(filename));
        } catch (IOException ignored) {
            // A missing old image must not prevent the profile operation.
        }
    }
}
