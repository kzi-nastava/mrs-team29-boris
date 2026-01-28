package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // Check if a vehicle with this registration already exists
    boolean existsByRegistration(String registration);
}
