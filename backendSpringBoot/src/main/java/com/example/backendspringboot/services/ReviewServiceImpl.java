package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.ReviewRequestDTO;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Review;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.ReviewRepository;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.services.interfaces.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final RideRepository rideRepository;
    private final ReviewRepository reviewRepository;
    private final PassengerRepository passengerRepository;

    @Override
    public List<ReviewRequestDTO> getAll(long rideId) {

        List<Review> reviews = reviewRepository.findByRideId(rideId);
        List<ReviewRequestDTO> dtos = new ArrayList<>();

        // Pack into DTO
        for (Review r : reviews) {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setRideId(r.getRide().getId());
            dto.setPassengerId(r.getPassenger().getId());
            dto.setDriverRating(r.getDriverRating());
            dto.setVehicleRating(r.getVehicleRating());
            dto.setComment(r.getComment());

            dtos.add(dto);
        }

        return dtos;
    }

    @Override
    public void createReview(ReviewRequestDTO dto, Long authenticatedPassengerId) {
        // Find ride
        Ride ride = rideRepository.findById(dto.getRideId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        if (ride.getRideCreator() == null
                || !ride.getRideCreator().getId().equals(authenticatedPassengerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Samo putnik koji je poručio vožnju može da je oceni.");
        }
        if (ride.getStatus() != com.example.backendspringboot.model.RideStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Može se oceniti samo završena vožnja.");
        }

        // Already rated?
        if (reviewRepository.existsByRideIdAndPassengerId(
                dto.getRideId(), authenticatedPassengerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Već ste ocenili ovu vožnju.");
        }

        // Time check
        if (ride.getEndTime() == null || LocalDateTime.now().isAfter(ride.getEndTime().plusHours(72))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Rok od tri dana za ocenjivanje je istekao.");
        }

        // Save data
        Passenger passenger = passengerRepository.findById(authenticatedPassengerId)
                .orElseThrow(() -> new EntityNotFoundException("Passenger not found"));

        Review review = new Review();
        review.setRide(ride);
        review.setPassenger(passenger);
        review.setDriverRating(dto.getDriverRating());
        review.setVehicleRating(dto.getVehicleRating());
        review.setComment(dto.getComment());
        review.setCreationTime(LocalDateTime.now());

        reviewRepository.save(review);
    }
}
