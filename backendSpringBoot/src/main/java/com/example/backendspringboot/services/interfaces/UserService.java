package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.request.ChangePasswordRequest;
import com.example.backendspringboot.dto.request.LoginRequestDTO;
import com.example.backendspringboot.dto.request.UpdateUserProfileRequestDTO;
import com.example.backendspringboot.dto.request.UserRegistrationRequestDTO;
import com.example.backendspringboot.dto.response.LoginResponseDTO;
import com.example.backendspringboot.dto.response.ProfileImageResponseDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import com.example.backendspringboot.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    LoginResponseDTO login(LoginRequestDTO request);

    UserProfileResponseDTO getUserProfile(Long id);
    UserProfileResponseDTO changeUserInfo(Long id, UpdateUserProfileRequestDTO request);
    void changePassword(Long id, ChangePasswordRequest request);
    UserProfileResponseDTO registerPassenger(UserRegistrationRequestDTO request);
    boolean activatePassenger(String token);
    ProfileImageResponseDTO uploadProfileImage(Long id, MultipartFile file);
    void deleteProfileImage(Long id);
    UserProfileResponseDTO blockUser(Long id, String reason);
    UserProfileResponseDTO unblockUser(Long id);
    UserProfileResponseDTO setNote(Long id, String reason);


    User findByEmail(String senderEmail);
}
