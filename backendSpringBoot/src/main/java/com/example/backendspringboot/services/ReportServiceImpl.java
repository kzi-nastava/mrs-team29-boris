package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.DailyStatsDTO;
import com.example.backendspringboot.dto.SummaryStatsDTO;
import com.example.backendspringboot.dto.request.ReportRequestDTO;
import com.example.backendspringboot.dto.response.ReportResponseDTO;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.GuestRide;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.Ride;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.UserRepository;
import com.example.backendspringboot.repositories.GuestRideRepository;
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
    private final GuestRideRepository guestRideRepository;

    private record ReportRide(LocalDateTime endTime, double kilometers, double money) { }

    public ReportResponseDTO generateReport(ReportRequestDTO request, String currentUserEmail, String currentUserRole) {

        if (request == null || request.getDateFrom() == null || request.getDateTo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range is required");
        }
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date FROM cannot be after date TO");
        }

        LocalDateTime dateFrom = request.getDateFrom().toLocalDate().atStartOfDay();
        LocalDateTime dateTo = request.getDateTo().toLocalDate().atTime(23, 59, 59);

        List<ReportRide> rides;
        boolean isEarnings = false;

        if ("ADMIN".equals(currentUserRole)) {
            rides = getAdminRides(request, dateFrom, dateTo);
            isEarnings = isEarningsReport(request.getUserType());
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
                rides = driverRides(user.getId(), dateFrom, dateTo);
                isEarnings = true;
            }
            else if ("PASSENGER".equals(currentUserRole)) {
                // Is he a passenger
                if (!(user instanceof Passenger)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logged in user is not a Passenger");
                }
                rides = regularRides(rideRepository.findFinishedRidesByPassengerAndDateRange(
                        user.getId(), dateFrom, dateTo));
                isEarnings = false;
            }
            else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user role: " + currentUserRole);
            }
        }

        return calculateReportStats(rides, dateFrom, dateTo, isEarnings);
    }

    private List<ReportRide> getAdminRides(ReportRequestDTO request, LocalDateTime dateFrom, LocalDateTime dateTo) {
        String userType = request.getUserType();

        if (userType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User type is required for Admin reports.");
        }

        if ("ALL_DRIVERS".equals(userType)) {
            List<ReportRide> rides = new ArrayList<>(regularRides(
                    rideRepository.findAllFinishedRidesByDriversAndDateRange(dateFrom, dateTo)));
            rides.addAll(guestRides(guestRideRepository.findAllFinishedByDriversAndDateRange(
                    dateFrom, dateTo)));
            return rides;
        } else if ("ALL_PASSENGERS".equals(userType)) {
            return regularRides(rideRepository.findAllFinishedRidesAndDateRange(dateFrom, dateTo));
        } else if (request.getUserId() != null) {
            // Single user report
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if ("DRIVER".equals(userType)) {
                if (!(user instanceof Driver)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a driver");
                }
                return driverRides(user.getId(), dateFrom, dateTo);
            } else if ("PASSENGER".equals(userType)) {
                if (!(user instanceof Passenger)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a passenger");
                }
                return regularRides(rideRepository.findFinishedRidesByPassengerAndDateRange(
                        user.getId(), dateFrom, dateTo));
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user type for single user report");
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report request parameters");
    }

    private static boolean isEarningsReport(String userType) {
        return "ALL_DRIVERS".equals(userType) || "DRIVER".equals(userType);
    }

    private ReportResponseDTO calculateReportStats(List<ReportRide> rides, LocalDateTime dateFrom, LocalDateTime dateTo, boolean isEarnings) {
        // Group rides by day
        Map<LocalDate, List<ReportRide>> ridesByDate = rides.stream()
                .collect(Collectors.groupingBy(ride -> ride.endTime().toLocalDate()));

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
            List<ReportRide> dayRides = ridesByDate.getOrDefault(date, Collections.emptyList());

            int numberOfRides = dayRides.size();

            double dayKm = dayRides.stream().mapToDouble(ReportRide::kilometers).sum();

            double dayMoney = dayRides.stream().mapToDouble(ReportRide::money).sum();

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

        return new ReportResponseDTO(dailyStats, summary, isEarnings);
    }

    private List<ReportRide> driverRides(Long driverId, LocalDateTime from, LocalDateTime to) {
        List<ReportRide> result = new ArrayList<>(regularRides(
                rideRepository.findFinishedRidesByDriverAndDateRange(driverId, from, to)));
        result.addAll(guestRides(guestRideRepository.findFinishedByDriverAndDateRange(
                driverId, from, to)));
        return result;
    }

    private static List<ReportRide> regularRides(List<Ride> rides) {
        return rides.stream()
                .filter(ride -> ride.getEndTime() != null)
                .map(ride -> new ReportRide(ride.getEndTime(),
                        ride.getRoute() == null ? 0 : ride.getRoute().getDistance(),
                        ride.getPrice()))
                .toList();
    }

    private static List<ReportRide> guestRides(List<GuestRide> rides) {
        return rides.stream()
                .filter(ride -> ride.getEndTime() != null)
                .map(ride -> new ReportRide(ride.getEndTime(),
                        ride.getRoute() == null ? 0 : ride.getRoute().getDistance(),
                        ride.getPrice()))
                .toList();
    }
}
