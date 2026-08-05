package com.example.backendspringboot.repositories;


import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.DriverStatus;
import com.example.backendspringboot.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    // Check to see if an email already exists
    boolean existsByEmail(String email);

    // Find driver by registration token
    Optional<Driver> findByRegistrationToken(String token);

    // Filter drivers by their status and if they are baby/pet friendly
//    @Query("""
//        SELECT DISTINCT d FROM Driver d
//        LEFT JOIN FETCH d.vehicle
//        LEFT JOIN FETCH d.scheduledRides
//        WHERE d.status = :status
//          AND d.vehicle.isBabyFriendly = :baby_bool
//          AND d.vehicle.isPetFriendly = :pet_bool
//        """)
    @Query("""
        SELECT DISTINCT d 
        FROM Driver d 
        LEFT JOIN FETCH d.vehicle
        LEFT JOIN FETCH d.scheduledRides
        LEFT JOIN FETCH d.activeRide
        WHERE d.status = :status 
        AND d.isBlocked = false
        AND d.deactivateAfterRide = false
        AND (:baby_bool = false OR d.vehicle.isBabyFriendly = true)
        AND (:pet_bool = false OR d.vehicle.isPetFriendly = true)
        AND d.vehicle.type = :vehicleType
    """)
    List<Driver> filterAvailableDrivers(@Param("status") DriverStatus status, @Param("baby_bool") boolean baby_bool, @Param("pet_bool") boolean pet_bool, @Param("vehicleType")VehicleType vehicleType);

    Optional<Driver> findByEmail(String email);
    Optional<Driver> findByVehicleId(Long vehicleId);
}
