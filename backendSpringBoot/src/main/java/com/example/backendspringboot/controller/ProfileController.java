package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.request.ProfilePasswordChangeRequestDTO;
import com.example.backendspringboot.dto.request.ProfileUpdateRequestDTO;
import com.example.backendspringboot.dto.response.DriverProfileChangeResponseDTO;
import com.example.backendspringboot.dto.response.OwnProfileResponseDTO;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping("/me")
    public OwnProfileResponseDTO getOwnProfile(Authentication authentication) {
        return profileService.getOwnProfile(principal(authentication));
    }

    @PutMapping("/me")
    public OwnProfileResponseDTO updateOwnProfile(Authentication authentication,
                                                  @Valid @RequestBody ProfileUpdateRequestDTO request) {
        return profileService.updateOwnProfile(principal(authentication), request);
    }

    @PostMapping("/me/image")
    public OwnProfileResponseDTO uploadImage(Authentication authentication,
                                             @RequestParam("file") MultipartFile file) {
        return profileService.uploadOwnImage(principal(authentication), file);
    }

    @DeleteMapping("/me/image")
    public OwnProfileResponseDTO deleteImage(Authentication authentication) {
        return profileService.deleteOwnImage(principal(authentication));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ProfilePasswordChangeRequestDTO request) {
        profileService.changePassword(principal(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/driver-change-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DriverProfileChangeResponseDTO> pendingChanges() {
        return profileService.pendingDriverChanges();
    }

    @PostMapping("/driver-change-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        profileService.approve(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/driver-change-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        profileService.reject(id);
        return ResponseEntity.noContent().build();
    }

    private User principal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof User user
                ? user : null;
    }
}
