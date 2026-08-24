package com.example.backendspringboot.services;

import com.example.backendspringboot.model.Administrator;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.Location;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Review;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.model.Route;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.dto.response.RideTrackingResponseDTO;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.InconsistencyReportRepository;
import com.example.backendspringboot.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RideTrackingAuthorizationTest {
    @Mock RideRepository rideRepository;
    @Mock InconsistencyReportRepository inconsistencyReportRepository;
    @Mock UserRepository userRepository;
    @InjectMocks RideServiceImpl service;

    @Test
    void onlyParticipantsDriverAndAdministratorCanTrackRide() {
        Passenger creator = passenger(1L);
        Passenger linked = passenger(2L);
        Passenger unrelated = passenger(3L);
        Driver driver = new Driver();
        driver.setId(4L);
        Administrator administrator = new Administrator();
        administrator.setId(5L);

        Ride ride = new Ride();
        ride.setRideCreator(creator);
        ride.setPassengers(List.of(linked));
        ride.setDriver(driver);
        when(rideRepository.findById(9L)).thenReturn(Optional.of(ride));

        assertDoesNotThrow(() -> service.assertCanTrackRide(9L, creator));
        assertDoesNotThrow(() -> service.assertCanTrackRide(9L, linked));
        assertDoesNotThrow(() -> service.assertCanTrackRide(9L, driver));
        assertDoesNotThrow(() -> service.assertCanTrackRide(9L, administrator));
        assertThrows(ResponseStatusException.class,
                () -> service.assertCanTrackRide(9L, unrelated));
    }

    @Test
    void finishedRideOffersReviewOnlyToCreatorWithinDeadline() {
        Passenger creator = passenger(1L);
        Passenger linked = passenger(2L);
        Route route = new Route();
        route.setOrigin(new Location(1L, 19.8, 45.2, "A"));
        route.setDestination(new Location(2L, 19.9, 45.3, "B"));
        route.setDuration(10);

        Ride ride = new Ride();
        ride.setId(9L);
        ride.setRideCreator(creator);
        ride.setPassengers(List.of(linked));
        ride.setStatus(RideStatus.FINISHED);
        ride.setEndTime(LocalDateTime.now().minusHours(1));
        ride.setRoute(route);
        ride.setStops(List.of(new Location(3L, 19.85, 45.25, "Stanica")));
        when(rideRepository.findById(9L)).thenReturn(Optional.of(ride));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(linked.getId())).thenReturn(Optional.of(linked));

        RideTrackingResponseDTO creatorResponse = service.getRideTracking(9L, creator);
        RideTrackingResponseDTO linkedResponse = service.getRideTracking(9L, linked);

        assertTrue(creatorResponse.isCanReview());
        assertFalse(linkedResponse.isCanReview());
        assertEquals("A", creatorResponse.getOrigin().getAddress());
        assertEquals("B", creatorResponse.getDestination().getAddress());
        assertEquals(1, creatorResponse.getStops().size());
        assertEquals("Stanica", creatorResponse.getStops().get(0).getAddress());

        Review review = new Review();
        review.setPassenger(creator);
        ride.setReviews(List.of(review));
        RideTrackingResponseDTO reviewedResponse = service.getRideTracking(9L, creator);
        assertFalse(reviewedResponse.isCanReview());
        assertTrue(reviewedResponse.isAlreadyReviewed());
    }

    @Test
    void databaseParticipantCheckAllowsPassengerWhenDetachedRideRelationsAreUnavailable() {
        Passenger passenger = passenger(7L);
        Ride ride = new Ride();
        ride.setId(13L);
        ride.setPassengers(List.of());
        when(rideRepository.findById(13L)).thenReturn(Optional.of(ride));
        when(rideRepository.existsRideParticipant(13L, 7L)).thenReturn(true);

        assertDoesNotThrow(() -> service.assertCanTrackRide(13L, passenger));
    }

    @Test
    void databaseParticipantCheckDoesNotDependOnConcretePrincipalSubclass() {
        User proxiedPrincipal = mock(User.class);
        when(proxiedPrincipal.getId()).thenReturn(7L);

        Ride ride = new Ride();
        ride.setId(12L);
        when(rideRepository.findById(12L)).thenReturn(Optional.of(ride));
        when(rideRepository.existsRideParticipant(12L, 7L)).thenReturn(true);

        assertDoesNotThrow(() -> service.assertCanTrackRide(12L, proxiedPrincipal));
    }

    private Passenger passenger(long id) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        return passenger;
    }
}
