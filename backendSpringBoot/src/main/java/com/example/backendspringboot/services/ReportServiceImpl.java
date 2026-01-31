package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.DailyStatsDTO;
import com.example.backendspringboot.dto.SummaryStatsDTO;
import com.example.backendspringboot.dto.request.ReportRequestDTO;
import com.example.backendspringboot.dto.response.ReportResponseDTO;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.repositories.DriverRepository;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    public ReportResponseDTO generateReport(ReportRequestDTO request, String currentUserEmail, String currentUserRole) {

        // Validate date
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date FROM cannot be after date TO");
        }

        LocalDateTime dateFrom = request.getDateFrom().toLocalDate().atStartOfDay();
        LocalDateTime dateTo = request.getDateTo().toLocalDate().atTime(23, 59, 59);

        List<Ride> rides;
        boolean isEarnings = false;

        if ("ADMIN".equals(currentUserRole)) {
            rides = getAdminRides(request, dateFrom, dateTo);
            isEarnings = isEarningsReport(request.getUserType(), request.getUserId());
        }
        else {
            // Find user in database
            User user = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + currentUserEmail));

            if ("DRIVER".equals(currentUserRole)) {
                // Check the user role, is he a driver
                if (!(user instanceof Driver)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logged in user is not a Driver");
                }
                rides = rideRepository.findFinishedRidesByDriverAndDateRange(user.getId(), dateFrom, dateTo);
                isEarnings = true;
            }
            else if ("PASSENGER".equals(currentUserRole)) {
                // Is he a passenger
                if (!(user instanceof Passenger)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logged in user is not a Passenger");
                }
                rides = rideRepository.findFinishedRidesByPassengerAndDateRange(user.getId(), dateFrom, dateTo);
                isEarnings = false;
            }
            else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user role: " + currentUserRole);
            }
        }

        return calculateReportStats(rides, dateFrom, dateTo, isEarnings);
    }

    private List<Ride> getAdminRides(ReportRequestDTO request, LocalDateTime dateFrom, LocalDateTime dateTo) {
        String userType = request.getUserType();

        if (userType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User type is required for Admin reports.");
        }

        if ("ALL_DRIVERS".equals(userType)) {
            return rideRepository.findAllFinishedRidesByDriversAndDateRange(dateFrom, dateTo);
        } else if ("ALL_PASSENGERS".equals(userType)) {
            return rideRepository.findAllFinishedRidesAndDateRange(dateFrom, dateTo);
        } else if (request.getUserId() != null) {
            // Single user report
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if ("DRIVER".equals(userType)) {
                return rideRepository.findFinishedRidesByDriverAndDateRange(user.getId(), dateFrom, dateTo);
            } else if ("PASSENGER".equals(userType)) {
                return rideRepository.findFinishedRidesByPassengerAndDateRange(user.getId(), dateFrom, dateTo);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user type for single user report");
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report request parameters");
    }

    private boolean isEarningsReport(String userType, Long userId) {
        if (userType.equals("ALL_DRIVERS") || userType.equals("DRIVER")) {
            return true;
        } else if (userType.equals("ALL_PASSENGERS") || userType.equals("PASSENGER")) {
            return false;
        } else if (userId != null) {
            // Check if user is passenger or driver
            User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            return user instanceof  Driver;
        }

        return false;
    }

    private ReportResponseDTO calculateReportStats(List<Ride> rides, LocalDateTime dateFrom, LocalDateTime dateTo, boolean isEarnings) {
        // Group rides by day
        Map<LocalDate, List<Ride>> ridesByDate = rides.stream().collect(Collectors.groupingBy(ride -> ride.getEndTime().toLocalDate()));

        List<DailyStatsDTO> dailyStats = new ArrayList<>();

        long daysBetween = ChronoUnit.DAYS.between(dateFrom.toLocalDate(), dateTo.toLocalDate()) + 1;

        double totalKm = 0;
        double totalMoney = 0;
        int totalRides = 0;

        // Cumulative values
        int cumulativeRides = 0;
        double cumulativeKm = 0;
        double cumulativeMoney = 0;

        for (LocalDate date = dateFrom.toLocalDate(); !date.isAfter(dateTo.toLocalDate()); date = date.plusDays(1)) {
            List<Ride> dayRides = ridesByDate.getOrDefault(date, Collections.emptyList());

            int numberOfRides = dayRides.size();

            double dayKm = dayRides.stream().mapToDouble(r -> r.getRoute() != null ? r.getRoute().getDistance() : 0).sum();

            double dayMoney = dayRides.stream().mapToDouble(Ride::getPrice).sum();

            if (!isEarnings) {
                dayMoney = -dayMoney; // Passengers are spending money
            }

            cumulativeRides += numberOfRides;
            cumulativeKm += dayKm;
            cumulativeMoney += dayMoney;

            dailyStats.add(new DailyStatsDTO(date, numberOfRides, dayKm, dayMoney, cumulativeRides, cumulativeKm, cumulativeMoney));

            totalRides += numberOfRides;
            totalKm += dayKm;
            totalMoney += dayMoney;
        }

        // Calculate avergae
        SummaryStatsDTO summary = new SummaryStatsDTO(
                totalRides,
                Math.round(totalKm * 100.0) / 100.0,
                Math.round(totalMoney * 100.0) / 100.0,
                Math.round((double) totalRides / daysBetween * 100.0) / 100.0,
                Math.round((totalKm / daysBetween) * 100.0) / 100.0,
                Math.round((totalMoney / daysBetween) * 100.0) / 100.0
        );

        return new ReportResponseDTO(dailyStats, summary);
    }
}
