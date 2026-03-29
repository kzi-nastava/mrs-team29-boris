package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.request.*;
import com.example.backendspringboot.dto.response.LoginResponseDTO;
import com.example.backendspringboot.dto.response.ProfileImageResponseDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import com.example.backendspringboot.dto.response.DriverStatusResponseDTO;
import com.example.backendspringboot.model.Administrator;
import com.example.backendspringboot.model.Gender;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.repositories.AdministratorRepository;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.UserRepository;
import com.example.backendspringboot.security.JwtUtil;
import com.example.backendspringboot.services.interfaces.UserService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.backendspringboot.services.interfaces.UserService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final AdministratorRepository administratorRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;

    public UserController(UserService userService, AdministratorRepository administratorRepository, JwtUtil jwtUtil, UserRepository userRepository, PassengerRepository passengerRepository){
        this.userService = userService;
        this.administratorRepository = administratorRepository;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passengerRepository = passengerRepository;
    }

    // GET profile
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('USER','DRIVER','ADMIN')")
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(@PathVariable Long id) {

        UserProfileResponseDTO response = userService.getUserProfile(id);
        return ResponseEntity.ok(response);
    }

    // PUT profile change
    @PutMapping("/{id}/profile-update")
    @PreAuthorize("hasAnyRole('USER','DRIVER','ADMIN')")
    public ResponseEntity<UserProfileResponseDTO> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserProfileRequestDTO request) {
        UserProfileResponseDTO response = userService.changeUserInfo(id, request);
        return ResponseEntity.ok(response);
    }

    // Endpoint for changing users password
    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('USER','DRIVER','ADMIN')")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request) {

        userService.changePassword(request.getId(), request);
        return ResponseEntity.ok(Collections.singletonMap("message", "Password changed successfully"));
    }

    // Allow user to upload a photo
    @PostMapping("/{id}/profile-image")
    @PreAuthorize("hasAnyRole('USER', 'DRIVER', 'ADMIN')")
    public ResponseEntity<ProfileImageResponseDTO> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        ProfileImageResponseDTO response = userService.uploadProfileImage(id, file);
        return ResponseEntity.ok(response);
    }

    // Delete profile image
    @DeleteMapping("/{id}/delete-profile-image")
    @PreAuthorize("hasAnyRole('USER', 'DRIVER', 'ADMIN')")
    public ResponseEntity<Void> deleteProfileImage(@PathVariable Long id) {
        userService.deleteProfileImage(id);
        return ResponseEntity.noContent().build();
    }

    // Get profile image
    @GetMapping("/profile-images/{filename}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/profile-images").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponseDTO> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoginResponseDTO response = new LoginResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getClass().getSimpleName(),
                token,
                user.isBlocked(),
                user.getBlockReason()
        );

        return ResponseEntity.ok(response);
    }

    // POST: Login korisnika
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/auth/test")
    public String test() {
        return "Test endpoint radi!";
    }

    @GetMapping("/auth/secure-test")
    public String secureTest() {
        return "Secure endpoint radi, JWT potreban!";
    }

    @PostMapping("/test-admin")
    public String testAdmin() {
        Administrator admin = administratorRepository.findAll().get(0);
        return admin.getEmail();
    }

    // POST: Logout korisnika
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.logout(authenticatedEmail(authentication));
        return ResponseEntity.noContent().build();
    }

    // POST: Zaboravljena lozinka
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    // POST: Reset lozinke
    @PostMapping("/auth/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        userService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    // PATCH: Promena statusa vozača
    @PatchMapping("/drivers/{id}/status")
    public ResponseEntity<DriverStatusResponseDTO> changeDriverStatus(
            @PathVariable Long id,
            @RequestBody DriverStatusRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.changeDriverStatus(
                id, request, authenticatedEmail(authentication)));
    }

    @GetMapping("/drivers/{id}/status")
    public ResponseEntity<DriverStatusResponseDTO> getDriverStatus(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.getDriverStatus(
                id, authenticatedEmail(authentication)));
    }

    static String authenticatedEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getEmail();
        }
        return authentication.getName();
    }

    // POST: Registracija korisnika
    @PostMapping("/auth/register")
    public ResponseEntity<UserProfileResponseDTO> registerUser(
            @Valid @RequestBody UserRegistrationRequestDTO request
    ) {
        UserProfileResponseDTO response = userService.registerPassenger(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET: Aktivacija korisnickog naloga
    @GetMapping("/auth/activate/{token}")
    public ResponseEntity<Void> activateUser(@PathVariable String token) {
        userService.activatePassenger(token);
        return ResponseEntity.ok().build();
    }
}
