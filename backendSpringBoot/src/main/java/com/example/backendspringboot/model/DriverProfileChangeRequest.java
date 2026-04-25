package com.example.backendspringboot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class DriverProfileChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileChangeStatus status = ProfileChangeStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String name;
    private String surname;
    private String email;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    private String address;
    private String phone;
    private String profileImageUrl;
    private boolean removeProfileImage;

    private String vehicleModel;
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;
    private String vehicleRegistration;
    private Integer vehicleSeats;
    private Boolean babyFriendly;
    private Boolean petFriendly;
}
