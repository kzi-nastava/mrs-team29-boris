package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.DriverProfileChangeRequest;
import com.example.backendspringboot.model.ProfileChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverProfileChangeRequestRepository
        extends JpaRepository<DriverProfileChangeRequest, Long> {
    Optional<DriverProfileChangeRequest> findFirstByDriverIdAndStatus(
            Long driverId, ProfileChangeStatus status);
    List<DriverProfileChangeRequest> findAllByStatusOrderByCreatedAtAsc(ProfileChangeStatus status);
}
