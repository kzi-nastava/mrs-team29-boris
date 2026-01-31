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
import org.springframework.beans.factory.annotation.Autowired;
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
    public void createReview(ReviewRequestDTO dto) {
        // Find ride
        Ride ride = rideRepository.findById(dto.getRideId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        // Check if passenger participated
        boolean participated = ride.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(dto.getPassengerId()));
        if(!participated){
            participated = ride.getRideCreator().getId().equals(dto.getPassengerId());
        }
        if (!participated) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You did not participate in this ride.");
        }

        // Already rated?
        if (reviewRepository.existsByRideIdAndPassengerId(dto.getRideId(), dto.getPassengerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already rated this ride.");
        }

        // Time check
        if (ride.getEndTime() == null || LocalDateTime.now().isAfter(ride.getEndTime().plusHours(72))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review deadline has passed.");
        }

        // Save data
        Passenger passenger = passengerRepository.findById(dto.getPassengerId())
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
