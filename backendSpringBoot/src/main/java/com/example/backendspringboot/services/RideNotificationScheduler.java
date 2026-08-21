package com.example.backendspringboot.services;

import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.RideStatus;
import com.example.backendspringboot.repositories.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RideNotificationScheduler {
    private final RideRepository rideRepository;
    private final AppNotificationService notificationService;

    @Value("${app.ride.reminder-thresholds-seconds:900,600,300}")
    private String reminderThresholdsSeconds = "900,600,300";

    @Scheduled(fixedRateString = "${app.ride.reminder-check-rate-ms:30000}")
    @Transactional
    public void checkUpcomingRides() {
        LocalDateTime now = LocalDateTime.now();
        List<Ride> rides = rideRepository.findAllByStatus(RideStatus.SCHEDULED);

        for (Ride ride : rides) {
            long seconds = ChronoUnit.SECONDS.between(now, ride.getScheduledTime());
            for (long threshold : thresholds()) {
                // Event keys make this safe on every scheduler pass. Using <= instead
                // of exact equality prevents a five-second polling interval from
                // skipping a reminder boundary.
                if (seconds > 0 && seconds <= threshold) {
                    sendNotification(ride, threshold);
                }
            }
        }
    }

    private List<Long> thresholds() {
        return Arrays.stream(reminderThresholdsSeconds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::parseLong)
                .sorted(java.util.Comparator.reverseOrder())
                .toList();
    }

    private void sendNotification(Ride ride, long thresholdSeconds) {
        String content = "Podsetnik: zakazana vožnja počinje za "
                + displayThreshold(thresholdSeconds) + ".";
        Passenger creator = ride.getRideCreator();
        notificationService.notify(creator, ride, "RIDE_REMINDER", content,
                reminderKey(ride, creator, thresholdSeconds));
        if (ride.getPassengers() == null) return;
        for (Passenger p : ride.getPassengers()) {
            if (!p.getId().equals(creator.getId())) {
                notificationService.notify(p, ride, "RIDE_REMINDER", content,
                        reminderKey(ride, p, thresholdSeconds));
            }
        }
    }

    private static String displayThreshold(long seconds) {
        if (seconds >= 120 && seconds % 60 == 0) {
            return (seconds / 60) + " minuta";
        }
        return seconds + " sekundi";
    }

    private String reminderKey(Ride ride, Passenger passenger, long thresholdSeconds) {
        return "ride:" + ride.getId() + ":reminder:"
                + thresholdSeconds + ":" + passenger.getId();
    }

    void useReminderThresholdsForTest(String thresholds) {
        this.reminderThresholdsSeconds = thresholds;
    }
}
