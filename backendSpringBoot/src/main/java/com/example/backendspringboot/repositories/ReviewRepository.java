package com.example.backendspringboot.repositories;


import com.example.backendspringboot.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByRideIdAndPassengerId(Long rideId, Long passengerId);
    List<Review> findByRideId(Long rideId);

}