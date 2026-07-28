package com.example.backendspringboot.repositories;


import com.example.backendspringboot.model.GuestRide;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;

@Repository
public interface GuestRideRepository extends JpaRepository<GuestRide, Long> {
    List<GuestRide> findAllByStatus(RideStatus status);
    List<GuestRide> findAllByDriverId(Long driverId);
    Page<GuestRide> findAllByDriverId(Long driverId, Pageable pageable);

    @Query("SELECT r FROM GuestRide r WHERE r.driver.id = :driverId " +
            "AND r.status = 'FINISHED' AND r.endTime BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY r.endTime")
    List<GuestRide> findFinishedByDriverAndDateRange(
            @Param("driverId") Long driverId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo);

    @Query("SELECT r FROM GuestRide r WHERE r.driver IS NOT NULL " +
            "AND r.status = 'FINISHED' AND r.endTime BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY r.endTime")
    List<GuestRide> findAllFinishedByDriversAndDateRange(
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo);
}
