package com.example.backendspringboot.services;

import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.repositories.RideRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RideNotificationSchedulerTest {
    @Test
    void fifteenMinuteReminderIsCreatedForCreatorAndLinkedPassengers() {
        RideRepository rideRepository = mock(RideRepository.class);
        AppNotificationService notifications = mock(AppNotificationService.class);
        RideNotificationScheduler scheduler = new RideNotificationScheduler(
                rideRepository, notifications);
        Passenger creator = passenger(1L);
        Passenger linked = passenger(2L);
        Ride ride = new Ride();
        ride.setId(8L);
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setScheduledTime(LocalDateTime.now().plusMinutes(14).plusSeconds(30));
        ride.setRideCreator(creator);
        ride.setPassengers(List.of(linked));
        when(rideRepository.findAllByStatus(RideStatus.SCHEDULED)).thenReturn(List.of(ride));

        scheduler.checkUpcomingRides();

        verify(notifications).notify(eq(creator), eq(ride), eq("RIDE_REMINDER"),
                contains("15 minuta"), eq("ride:8:reminder:15:1"));
        verify(notifications).notify(eq(linked), eq(ride), eq("RIDE_REMINDER"),
                contains("15 minuta"), eq("ride:8:reminder:15:2"));
    }

    private Passenger passenger(long id) {
        Passenger value = new Passenger();
        value.setId(id);
        return value;
    }
}
