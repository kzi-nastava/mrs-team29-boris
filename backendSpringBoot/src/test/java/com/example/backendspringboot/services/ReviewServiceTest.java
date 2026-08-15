package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.request.ReviewRequestDTO;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Review;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.ReviewRepository;
import com.example.backendspringboot.repositories.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock RideRepository rideRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock PassengerRepository passengerRepository;
    @InjectMocks ReviewServiceImpl service;

    private Passenger creator;
    private Ride ride;
    private ReviewRequestDTO request;

    @BeforeEach
    void setUp() {
        creator = new Passenger();
        creator.setId(10L);
        ride = new Ride();
        ride.setId(5L);
        ride.setRideCreator(creator);
        ride.setStatus(RideStatus.FINISHED);
        ride.setEndTime(LocalDateTime.now().minusHours(1));
        request = new ReviewRequestDTO(null, null, 4, 5, "Uredna vožnja");
        request.setRideId(5L);
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride));
    }

    @Test
    void creatorCanRateFinishedRideWithinThreeDays() {
        when(passengerRepository.findById(10L)).thenReturn(Optional.of(creator));

        service.createReview(request, 10L);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review review = captor.getValue();
        assertSame(ride, review.getRide());
        assertSame(creator, review.getPassenger());
        assertEquals(4, review.getDriverRating());
        assertEquals(5, review.getVehicleRating());
        assertEquals("Uredna vožnja", review.getComment());
    }

    @Test
    void linkedPassengerCannotRateRideOrderedByCreator() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createReview(request, 11L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unfinishedRideCannotBeRated() {
        ride.setStatus(RideStatus.STARTED);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createReview(request, 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void rideCannotBeRatedTwice() {
        when(reviewRepository.existsByRideIdAndPassengerId(5L, 10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createReview(request, 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void reviewIsRejectedAfterThreeDayDeadline() {
        ride.setEndTime(LocalDateTime.now().minusDays(3).minusSeconds(1));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createReview(request, 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
