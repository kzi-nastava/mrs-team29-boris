package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.PanicRequestDTO;
import com.example.backendspringboot.dto.response.PanicResponseDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import com.example.backendspringboot.services.interfaces.PanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PanicServiceImpl implements PanicService {

    private final PanicRepository panicRepository;
    private final RideRepository rideRepository;
    private final GuestRideRepository guestRideRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PanicResponseDTO triggerPanic(PanicRequestDTO request) {

        System.out.println("=== PANIC SERVICE ===");
        System.out.println("Ride ID: " + request.getRideId());
        System.out.println("Is Guest: " + request.isGuest());
        System.out.println("User ID: " + request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Panic panic = new Panic();
        panic.setCreatedAt(LocalDateTime.now());
        panic.setTriggeredBy(user);
        panic.setResolved(false);

        String originAddress = "";
        String destinationAddress = "";

        if (request.isGuest()) {
            System.out.println("Looking for GuestRide with ID: " + request.getRideId());
            GuestRide guestRide = guestRideRepository.findById(request.getRideId())
                    .orElseThrow(() -> {
                        System.out.println("GuestRide NOT FOUND!");
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest ride not found");
                    });

            System.out.println("GuestRide found! Status: " + guestRide.getStatus());

            if (guestRide.getStatus() == RideStatus.FINISHED || guestRide.getStatus() == RideStatus.CANCELED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot trigger panic for finished or canceled ride");
            }

            panic.setGuestRide(guestRide);
            guestRide.setPanicPressed(true);
            guestRideRepository.save(guestRide);

            originAddress = guestRide.getRoute().getOrigin().getAddress();
            destinationAddress = guestRide.getRoute().getDestination().getAddress();

        } else {
            System.out.println("Looking for Ride with ID: " + request.getRideId());
            Ride ride = rideRepository.findById(request.getRideId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
            System.out.println("Ride found! Status: " + ride.getStatus());

            if (ride.getStatus() == RideStatus.FINISHED || ride.getStatus() == RideStatus.CANCELED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot trigger panic for finished or canceled ride");
            }

            panic.setRide(ride);
            ride.setPanicPressed(true);
            rideRepository.save(ride);

            originAddress = ride.getRoute().getOrigin().getAddress();
            destinationAddress = ride.getRoute().getDestination().getAddress();
        }

        panicRepository.save(panic);

        return new PanicResponseDTO(
                panic.getId(),
                request.getRideId(),
                request.isGuest(),
                user.getName() + " " + user.getSurname(),
                user.getEmail(),
                panic.getCreatedAt(),
                panic.isResolved(),
                originAddress,
                destinationAddress
        );
    }

    @Override
    public List<PanicResponseDTO> getAllPanics() {
        return panicRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<PanicResponseDTO> getUnresolvedPanics() {
        List<Panic> panics = panicRepository.findAllUnresolvedWithRelations();
        System.out.println("PANICS RECEIVED: " + panics.size()); // DEBUG
        return panics.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void resolvePanic(Long panicId) {
        Panic panic = panicRepository.findById(panicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Panic not found"));

        panic.setResolved(true);
        panicRepository.save(panic);
    }

    private PanicResponseDTO mapToDTO(Panic panic) {
        Long rideId = null;
        boolean isGuest = false;
        String originAddress = "";
        String destinationAddress = "";

        if (panic.getRide() != null) {
            rideId = panic.getRide().getId();
            isGuest = false;
            if (panic.getRide().getRoute() != null) {
                originAddress = panic.getRide().getRoute().getOrigin().getAddress();
                destinationAddress = panic.getRide().getRoute().getDestination().getAddress();
            }
        } else if (panic.getGuestRide() != null) {
            rideId = panic.getGuestRide().getId();
            isGuest = true;
            if (panic.getGuestRide().getRoute() != null) {
                originAddress = panic.getGuestRide().getRoute().getOrigin().getAddress();
                destinationAddress = panic.getGuestRide().getRoute().getDestination().getAddress();
            }
        }

        String triggeredByName = panic.getTriggeredBy() != null
                ? panic.getTriggeredBy().getName() + " " + panic.getTriggeredBy().getSurname()
                : "Unknown";
        String triggeredByEmail = panic.getTriggeredBy() != null
                ? panic.getTriggeredBy().getEmail()
                : "Unknown";

        return new PanicResponseDTO(
                panic.getId(),
                rideId,
                isGuest,
                triggeredByName,
                triggeredByEmail,
                panic.getCreatedAt(),
                panic.isResolved(),
                originAddress,
                destinationAddress
        );
    }
}