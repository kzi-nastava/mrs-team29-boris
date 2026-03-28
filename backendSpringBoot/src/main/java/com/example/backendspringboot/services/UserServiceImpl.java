package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.ChangePasswordRequest;
import com.example.backendspringboot.dto.request.DriverStatusRequestDTO;
import com.example.backendspringboot.dto.request.LoginRequestDTO;
import com.example.backendspringboot.dto.request.ResetPasswordRequestDTO;
import com.example.backendspringboot.dto.request.UpdateUserProfileRequestDTO;
import com.example.backendspringboot.dto.response.*;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import com.example.backendspringboot.dto.request.UserRegistrationRequestDTO;
import com.example.backendspringboot.dto.response.LoginResponseDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.UserRepository;
import com.example.backendspringboot.security.JwtUtil;
import com.example.backendspringboot.services.interfaces.EmailService;
import com.example.backendspringboot.services.interfaces.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final VehicleRepository vehicleRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Path fileStorageLocation = Paths.get("uploads/profile-images");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public UserServiceImpl(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            PassengerRepository passengerRepository,
            EmailService emailService,
            JwtUtil jwtUtil, 
            VehicleRepository vehicleRepository
            ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.passengerRepository = passengerRepository;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.vehicleRepository = vehicleRepository;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user instanceof Passenger passenger && !passenger.isActivated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not activated. Check your email.");
        }

        if (user instanceof Driver driver && driver.getStatus() == DriverStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Driver registration is not completed");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }

        if (user instanceof Driver driver) {
            driver.setStatus(DriverStatus.ACTIVE);
            driver.setDeactivateAfterRide(false);
            userRepository.save(driver);
        }

        String role = user.getClass().getSimpleName().toLowerCase();
        if (role.equals("administrator")) {
            role = "admin";
        } else if (role.equals("passenger")) {
            role = "user";
        } else if (role.equals("driver")) {
            role = "driver";
        }

        String token = jwtUtil.generateToken(user);

        return new LoginResponseDTO(user.getId(), user.getEmail(), role, token, user.isBlocked(), user.getBlockReason());
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user instanceof Driver driver) {
            if (driver.getActiveRide() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Driver cannot log out during an active ride");
            }
            driver.setStatus(DriverStatus.INACTIVE);
            driver.setDeactivateAfterRide(false);
            userRepository.save(driver);
        }
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            EmailDetails details = new EmailDetails();
            details.setRecipient(user.getEmail());
            details.setSubject("Reset your ClickAndDrive password");
            details.setMsgBody(
                    "Open this link to set a new password:\n\n"
                            + "clickanddrive://reset-password?token=" + token
                            + "\n\nThe link expires in one hour."
            );
            emailService.sendsSimpleMail(details);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid password reset token"));
        if (user.getPasswordResetTokenExpiry() == null
                || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    @Override
    public DriverStatusResponseDTO getDriverStatus(Long id, String requesterEmail) {
        Driver driver = getAuthorizedDriver(id, requesterEmail);
        return toDriverStatusResponse(driver);
    }

    @Override
    @Transactional
    public DriverStatusResponseDTO changeDriverStatus(Long id, DriverStatusRequestDTO request,
                                                       String requesterEmail) {
        Driver driver = getAuthorizedDriver(id, requesterEmail);
        if (request.getStatus() == null || request.getStatus() == DriverStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid driver status");
        }

        if (request.getStatus() == DriverStatus.ACTIVE) {
            driver.setStatus(DriverStatus.ACTIVE);
            driver.setDeactivateAfterRide(false);
        } else if (driver.getActiveRide() != null) {
            driver.setDeactivateAfterRide(true);
        } else {
            driver.setStatus(DriverStatus.INACTIVE);
            driver.setDeactivateAfterRide(false);
        }
        userRepository.save(driver);
        return toDriverStatusResponse(driver);
    }

    private Driver getAuthorizedDriver(Long id, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Driver driver = userRepository.findById(id)
                .filter(Driver.class::isInstance)
                .map(Driver.class::cast)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Driver not found"));
        if (!(requester instanceof Administrator) && !requester.getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return driver;
    }

    private DriverStatusResponseDTO toDriverStatusResponse(Driver driver) {
        return new DriverStatusResponseDTO(
                driver.getStatus(),
                driver.isDeactivateAfterRide(),
                driver.getActiveRide() != null
        );
    }

    public UserProfileResponseDTO getUserProfile(Long id) {
        // Find user in database based on his id
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("No user with this id exists"));

        // Map user data to response
        return new UserProfileResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getAddress(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.isBlocked(),
                user.getBlockReason()
        );
    }

    @Transactional
    @Override
    public UserProfileResponseDTO changeUserInfo(Long id, UpdateUserProfileRequestDTO request) {
        // Get user by id from database
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("No user with this id exists"));

        // Set changed information
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone());

        // Email check and validation
        if (!user.getEmail().equals(request.getEmail())) {
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        // Check users role, if driver, update his vehicle information as well
        String role = user.getClass().getSimpleName().toLowerCase();
        role = switch (role) {
            case "administrator" -> "admin";
            case "passenger" -> "user";
            case "driver" -> "driver";
            default -> role;
        };

        if (role.equals("driver") && request.getVehicle() != null) {
            Driver driver = (Driver) user;

            if (driver.getVehicle() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver does not have a vehicle assigned");
            }

            Vehicle vehicle = driver.getVehicle();
            // Update vehicle information
            vehicle.setModel(request.getVehicle().getModel());
            vehicle.setType(request.getVehicle().getType());
            vehicle.setIsBabyFriendly(request.getVehicle().getIsBabyFriendly());
            vehicle.setIsPetFriendly(request.getVehicle().getIsPetFriendly());

            // Validate registration, and see if unique
            if (!driver.getVehicle().getRegistration().equals(request.getVehicle().getRegistration())) {
                boolean existsRegistration = vehicleRepository.existsByRegistration(request.getVehicle().getRegistration());
                if (existsRegistration) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration table already exists");
                }
                vehicle.setRegistration(request.getVehicle().getRegistration());
            }
            // Save vehicle
            vehicleRepository.save(vehicle);
        }
        // Save user
        userRepository.save(user);

        return new UserProfileResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getAddress(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.isBlocked(),
                user.getBlockReason()
        );
    }

    @Override
    public void changePassword(Long id, ChangePasswordRequest request) {
        // Find user
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if current password matches the one in database
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        // Check if new password and confirmation are equal
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New passwords do not match");
        }

        // Save new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
      
    public UserProfileResponseDTO registerPassenger(UserRegistrationRequestDTO dto) {

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passwords do not match"
            );
        }

        if (passengerRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already exists"
            );
        }

        Passenger passenger = new Passenger();
        passenger.setName(dto.getName());
        passenger.setSurname(dto.getLastName());
        passenger.setEmail(dto.getEmail());
        passenger.setPassword(passwordEncoder.encode(dto.getPassword()));
        passenger.setAddress(dto.getAddress());
        passenger.setPhone(dto.getPhoneNumber());
        passenger.setGender(Gender.MALE);

        passenger.setActivated(false);
        passenger.setActivationToken(java.util.UUID.randomUUID().toString());
        passenger.setActivationTokenExpiry(LocalDateTime.now().plusHours(24));

        Passenger saved = passengerRepository.save(passenger);


        String activationLink;
        if (dto.isMobile()) {
            // URL za mobilnu aplikaciju, npr. deep link
            activationLink = "https://abcd1234.ngrok.io/activate-account?token=" + saved.getActivationToken();
        } else {
            // URL za web frontend
            activationLink = "http://localhost:4200/activate-account?token=" + saved.getActivationToken();
        }
        EmailDetails email = new EmailDetails();
        email.setRecipient(saved.getEmail());
        email.setSubject("Activate your account");
        email.setMsgBody(
                "Hello " + saved.getName() + ",\n\n" +
                        "Welcome to ClickAndDrive! Please complete your registration by setting your password:\n\n" +
                        activationLink + "\n\n" +
                        "This link will expire in 24 hours.\n\n" +
                        "Best regards,\nClickAndDrive Team"
        );

        emailService.sendsSimpleMail(email);

        return new UserProfileResponseDTO(
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                saved.getSurname(),
                saved.getAddress(),
                saved.getPhone(),
                saved.getProfileImageUrl(),
                saved.isBlocked(),
                saved.getBlockReason()
        );
    }

    // Upload profile image
    @Override
    public ProfileImageResponseDTO uploadProfileImage(Long id, MultipartFile file) {
        // Validations
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds maximum limit (5MB)");
        }

        try {
            User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

            // Delete old one if exists
            if (user.getProfileImageUrl() != null) {
                String oldFilename = user.getProfileImageUrl().substring(user.getProfileImageUrl().lastIndexOf("/") + 1);
                Path oldFile = fileStorageLocation.resolve(oldFilename);
                Files.deleteIfExists(oldFile);
            }

            // Generate new name
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = id + "_" + System.currentTimeMillis() + extension;

            // Save file
            Path targetLocation = fileStorageLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Update URL
            String imageUrl = "/api/user/profile-images/" + filename;
            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);

            return new ProfileImageResponseDTO(
                    imageUrl,
                    "Profile image updated successfully"
            );
        } catch (IOException e) {
            throw new RuntimeException("Faild to store file", e);
        }
    }

    // Delete user profile image
    @Override
    public void deleteProfileImage(Long id) {
        // Find user
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProfileImageUrl() != null) {
            try {
                String filename = user.getProfileImageUrl().substring(user.getProfileImageUrl().lastIndexOf("/")+1);

                // Delete file
                Path file = fileStorageLocation.resolve(filename);
                Files.deleteIfExists(file);

                // Delete URL from database
                user.setProfileImageUrl(null);
                userRepository.save(user);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete file", e);
            }
        }
    }

    @Override
    public UserProfileResponseDTO blockUser(Long id, String reason) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Block him
        user.setBlocked(true);
        if (reason != null && !reason.trim().isEmpty()) {
            user.setBlockReason(reason);
        }

        userRepository.save(user);

        // Using websockets send a message
        messagingTemplate.convertAndSend(
                "/topic/user/" + id + "/status",
                Map.of(
                        "blocked", true,
                        "reason", reason != null ? reason : "",
                        "timestamp", LocalDateTime.now().toString()
                )
        );

        System.out.println(reason);

        return new UserProfileResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getAddress(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.isBlocked(),
                user.getBlockReason()
        );
    }

    @Override
    public UserProfileResponseDTO unblockUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setBlocked(false);
        user.setBlockReason(null);

        userRepository.save(user);

        // Using websockets send a message
        messagingTemplate.convertAndSend(
                "/topic/user/" + id + "/status",
                Map.of(
                        "blocked", false,
                        "reason", "",
                        "timestamp", LocalDateTime.now().toString()
                )
        );

        return new UserProfileResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getAddress(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.isBlocked(),
                user.getBlockReason()
        );
    }

    @Override
    public UserProfileResponseDTO setNote(Long id, String reason) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setBlockReason(reason);
        userRepository.save(user);

        // Using websockets send updated message on why the user is blocked
        messagingTemplate.convertAndSend(
                "/topic/user/" + id + "/status",
                Map.of(
                        "blocked", user.isBlocked(),
                        "reason", reason != null ? reason : "",
                        "timestamp", LocalDateTime.now().toString()
                )
        );

        return new UserProfileResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getAddress(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.isBlocked(),
                user.getBlockReason()
        );
    }

    @Transactional
    public boolean activatePassenger(String token) {
        Passenger passenger = passengerRepository.findByActivationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        passenger.setActivated(true);
        passenger.setActivationToken(null);
        passenger.setActivationTokenExpiry(null);
        passengerRepository.save(passenger);

        return true;
    }

    @Override
    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return user;
    }


}
