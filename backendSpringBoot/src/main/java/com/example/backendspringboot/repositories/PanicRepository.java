package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.Panic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PanicRepository extends JpaRepository<Panic, Long> {
    List<Panic> findAllByResolvedFalse();
    List<Panic> findAllByOrderByCreatedAtDesc();
    @Query("SELECT p FROM Panic p " +
            "LEFT JOIN FETCH p.ride r " +
            "LEFT JOIN FETCH r.route " +
            "LEFT JOIN FETCH p.guestRide gr " +
            "LEFT JOIN FETCH gr.route " +
            "LEFT JOIN FETCH p.triggeredBy " +
            "WHERE p.resolved = false " +
            "ORDER BY p.createdAt DESC")
    List<Panic> findAllUnresolvedWithRelations();
}