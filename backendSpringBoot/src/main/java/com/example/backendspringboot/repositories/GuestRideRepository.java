package com.example.backendspringboot.repositories;


import com.example.backendspringboot.model.GuestRide;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestRideRepository extends JpaRepository<GuestRide, Long> {
    List<GuestRide> findAllByStatus(RideStatus status);
    List<GuestRide> findAllByDriverId(Long driverId);
    Page<GuestRide> findAllByDriverId(Long driverId, Pageable pageable);
}