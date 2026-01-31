package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.NotificationDTO;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.repositories.RideRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class RideNotificationScheduler {
    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // (60000ms)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkUpcomingRides() {
        LocalDateTime now = LocalDateTime.now();
        //System.out.println("Scheduler started: " + LocalDateTime.now());

        // All SCHEDULED rides
        List<Ride> rides = rideRepository.findAllByStatus(RideStatus.SCHEDULED);
        //System.out.println("Found scheduled rides: " + rides.size());

        for (Ride ride : rides) {
            long minutesUntilRide = ChronoUnit.MINUTES.between(now, ride.getScheduledTime());

            // 15 minutes before start time
            if (minutesUntilRide >= 14 && minutesUntilRide <= 16) {
                //System.out.println("Detected notification time");
                sendNotification(ride, "Your ride begins in 15 minutes.");
            }

            // Send notification every 5 minutes
            if (minutesUntilRide < 15 && minutesUntilRide > 0 && minutesUntilRide % 5 == 0) {
                //System.out.println("Detected notification time");
                sendNotification(ride, "Reminder: Your ride begins in " + minutesUntilRide + " minutes.");
            }
        }
    }

    private void sendNotification(Ride ride, String message) {
        // Send message to passenger
        // /topic/passenger/{id}/notes
        NotificationDTO notification = new NotificationDTO(message, ride.getId());

        // Send notification to ride creator
        Long creatorId = ride.getRideCreator().getId();
        messagingTemplate.convertAndSend("/topic/passenger/" + creatorId + "/notes", notification);
        //System.out.println("Notification sent to: " + creatorId);

        // Send notification to other passenger, if there are any linked to ride
        for (Passenger p : ride.getPassengers()) {
            messagingTemplate.convertAndSend("/topic/passenger/" + p.getId() + "/notes", notification);
            //System.out.println("Notification sent to passenger: " + p.getId());
        }
    }
}
