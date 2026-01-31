package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.response.AdminRideHistoryResponseDTO;
import com.example.backendspringboot.dto.response.AdminUserResponseDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminService {
    List<AdminRideHistoryResponseDTO> getRideHistory(
            Long driverId,
            String userType,
            String sortBy
    );
    List<AdminUserResponseDTO> getAllNonAdminUsers();

    Page<UserProfileResponseDTO> getAllDriversPaged(Pageable pageable);

    Page<UserProfileResponseDTO> getAllPassengersPaged(Pageable pageable);
}
