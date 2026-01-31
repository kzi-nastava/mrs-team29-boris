package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.LocationDTO;
import com.example.backendspringboot.dto.response.DriverRideHistoryResponseDTO;
import com.example.backendspringboot.dto.response.PassengerRideHistoryResponseDTO;
import com.example.backendspringboot.dto.response.RouteFromFavoritesResponseDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.PassengerRepository;
import com.example.backendspringboot.repositories.RideRepository;
import com.example.backendspringboot.repositories.RouteRepository;
import com.example.backendspringboot.services.interfaces.PassengerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;
    private final RouteRepository routeRepository;
    private final RideRepository rideRepository;

    @Override
    public Page<RouteFromFavoritesResponseDTO> getFavoriteRoutesForPassenger(Long passengerId, Pageable pageable) {
        // Find the passenger by id
        Passenger passenger = passengerRepository.findById(passengerId).orElseThrow(() -> new RuntimeException("Passenger not found"));

        // Get his favorite routes, if he has any
        List<Route> favoriteRoutes = passenger.getFavoriteRoutes();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), favoriteRoutes.size());
        List<Route> pageContent = favoriteRoutes.subList(start, end);

        // Map models to ResponseDTO and send to frontend
        List<RouteFromFavoritesResponseDTO> dtos = pageContent.stream()
                .map(route -> new RouteFromFavoritesResponseDTO(
                        route.getId(),
                        new LocationDTO(route.getOrigin().getLongitude(), route.getOrigin().getLatitude(), route.getOrigin().getAddress()),
                        new LocationDTO(route.getDestination().getLongitude(), route.getDestination().getLatitude(), route.getDestination().getAddress()),
                        route.getDistance(),
                        route.getDuration(),
                        route.getTimesUsed()
                )).toList();

        return new PageImpl<>(dtos, pageable, favoriteRoutes.size());
    }

    @Override
    @Transactional
    public void removeFromFavoriteRoutes(Long passengerId, Long routeId) {
        // Find passenger from id
        Passenger passenger = passengerRepository.findById(passengerId).orElseThrow(() -> new RuntimeException("Passenger not found"));

        boolean removed = passenger.getFavoriteRoutes()
                        .removeIf(route -> route.getId().equals(routeId));

        passengerRepository.save(passenger);
    }

    @Override
    public List<UserProfileResponseDTO> getAllPassengers() {
        List<Passenger> passengers = passengerRepository.findAll();

        return passengers.stream().map(this::mapPassengerToDTO).collect(Collectors.toList());
    }

    // Helper
    private UserProfileResponseDTO mapPassengerToDTO(Passenger passenger) {
        return new UserProfileResponseDTO(
                passenger.getId(),
                passenger.getEmail(),
                passenger.getName(),
                passenger.getSurname(),
                passenger.getAddress(),
                passenger.getPhone(),
                passenger.getProfileImageUrl(),
                passenger.isBlocked(),
                passenger.getBlockReason()
        );
    }

    @Override
    public List<PassengerRideHistoryResponseDTO> getPassengerRideHistory(Long passengerId) {

        List<Ride> rides = rideRepository.findAllByPassengerId(passengerId);
        List<PassengerRideHistoryResponseDTO> dtos = new ArrayList<>();

        for (Ride ride : rides) {

            if (ride.getStatus() != RideStatus.FINISHED
                    && ride.getStatus() != RideStatus.STOPPED) continue;

            PassengerRideHistoryResponseDTO dto = new PassengerRideHistoryResponseDTO();

            dto.setId(ride.getId());
            dto.setStartTime(ride.getStartTime());
            dto.setEndTime(ride.getEndTime());
            dto.setTotalPrice(ride.getPrice());

            if (ride.getDriver() != null) {
                dto.setDriverEmail(ride.getDriver().getEmail());
            }

            if (ride.getStatus() == RideStatus.FINISHED) {
                dto.setStatus("Completed");
            }
            if (ride.getStatus() == RideStatus.STOPPED) {
                dto.setStatus("Stopped");
            }

            if (ride.getRoute() != null) {
                Location start = ride.getRoute().getOrigin();
                Location end = ride.getRoute().getDestination();

                dto.setOrigin(new LocationDTO(
                        start.getLongitude(),
                        start.getLatitude(),
                        start.getAddress()
                ));

                dto.setDestination(new LocationDTO(
                        end.getLongitude(),
                        end.getLatitude(),
                        end.getAddress()
                ));
            }

            dtos.add(dto);
        }

        return dtos;
    }
}
