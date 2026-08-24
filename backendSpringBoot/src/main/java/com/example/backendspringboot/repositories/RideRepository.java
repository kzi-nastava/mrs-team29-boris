package com.example.backendspringboot.repositories;


import com.example.backendspringboot.model.GuestRide;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findAllByDriverId(Long driverId);
    Page<Ride> findAllByDriverId(Long driverId, Pageable pageable);
    List<Ride> findAllByStatus(RideStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(DISTINCT r.id) > 0 THEN true ELSE false END
            FROM Ride r
            LEFT JOIN r.passengers p
            WHERE r.status = 'STARTED'
              AND (r.rideCreator.id = :passengerId OR p.id = :passengerId)
            """)
    boolean existsStartedRideForPassenger(@Param("passengerId") Long passengerId);

    @Query("""
            SELECT CASE WHEN COUNT(DISTINCT r.id) > 0 THEN true ELSE false END
            FROM Ride r
            LEFT JOIN r.passengers p
            WHERE r.id = :rideId
              AND (r.rideCreator.id = :passengerId OR p.id = :passengerId)
            """)
    boolean existsRideParticipant(@Param("rideId") Long rideId,
                                  @Param("passengerId") Long passengerId);

    @Query("SELECT DISTINCT r FROM Ride r LEFT JOIN r.passengers p " +
            "WHERE (p.id = :passengerId OR r.rideCreator.id = :passengerId) " +
            "AND r.status = 'FINISHED' " +
            "AND r.endTime BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY r.endTime")
    List<Ride> findFinishedRidesByPassengerAndDateRange(@Param("passengerId") Long passengerId, @Param("dateFrom")LocalDateTime dateFrom, @Param("dateTo") LocalDateTime dateTo);

    @Query("SELECT r FROM Ride r WHERE r.driver.id = :driverId " +
            "AND r.status = 'FINISHED' " +
            "AND r.endTime BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY r.endTime")
    List<Ride> findFinishedRidesByDriverAndDateRange(@Param("driverId") Long driverId, @Param("dateFrom") LocalDateTime dateFrom, @Param("dateTo") LocalDateTime dateTo);

    @Query("SELECT r FROM Ride r WHERE r.driver IS NOT NULL " +
            "AND r.status = 'FINISHED' " +
            "AND r.endTime BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY r.endTime")
    List<Ride> findAllFinishedRidesByDriversAndDateRange(@Param("dateFrom") LocalDateTime dateFrom, @Param("dateTo") LocalDateTime dateTo);

    @Query("SELECT r FROM Ride r WHERE r.status = 'FINISHED' " +
            "AND r.endTime BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY r.endTime")
    List<Ride> findAllFinishedRidesAndDateRange(@Param("dateFrom") LocalDateTime dateFrom, @Param("dateTo") LocalDateTime dateTo);

    @Query("""
SELECT DISTINCT r FROM Ride r
LEFT JOIN r.passengers p
WHERE p.id = :passengerId
   OR r.rideCreator.id = :passengerId
""")
    List<Ride> findAllByPassengerId(@Param("passengerId") Long passengerId);

    List<Ride> findAllByDriverIdOrderByStartTimeDesc(Long driverId);

    @Query("""
SELECT DISTINCT r FROM Ride r
JOIN r.passengers p
WHERE p.id = :passengerId
ORDER BY r.startTime DESC
""")
    List<Ride> findAllByPassengerIdOrdered(@Param("passengerId") Long passengerId);
}
