package com.example.backendspringboot.repositories;

import com.example.backendspringboot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetToken(String token);
    boolean existsByEmail(String email);
    @Query("SELECT u FROM User u WHERE TYPE(u) <> Administrator ")
    List<User> findAllNonAdmins();
}
