package com.example.backendspringboot.services;

import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.repositories.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RideNotificationScheduler {
    private final RideRepository rideRepository;
    private final AppNotificationService notificationService;

    // (60000ms)
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkUpcomingRides() {
        LocalDateTime now = LocalDateTime.now();
        //System.out.println("Scheduler started: " + LocalDateTime.now());

        // All SCHEDULED rides
        List<Ride> rides = rideRepository.findAllByStatus(RideStatus.SCHEDULED);
        //System.out.println("Found scheduled rides: " + rides.size());

        for (Ride ride : rides) {
            long seconds = ChronoUnit.SECONDS.between(now, ride.getScheduledTime());
            long minutesUntilRide = (long) Math.ceil(seconds / 60.0);
            if (minutesUntilRide == 15 || minutesUntilRide == 10 || minutesUntilRide == 5) {
                sendNotification(ride, minutesUntilRide);
            }
        }
    }

    private void sendNotification(Ride ride, long minutes) {
        String content = "Podsetnik: zakazana vožnja počinje za " + minutes + " minuta.";
        Passenger creator = ride.getRideCreator();
        notificationService.notify(creator, ride, "RIDE_REMINDER", content,
                reminderKey(ride, creator, minutes));
        if (ride.getPassengers() == null) return;
        for (Passenger p : ride.getPassengers()) {
            if (!p.getId().equals(creator.getId())) {
                notificationService.notify(p, ride, "RIDE_REMINDER", content,
                        reminderKey(ride, p, minutes));
            }
        }
    }

    private String reminderKey(Ride ride, Passenger passenger, long minutes) {
        return "ride:" + ride.getId() + ":reminder:" + minutes + ":" + passenger.getId();
    }
}
