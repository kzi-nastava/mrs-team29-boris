package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.response.PassengerRideHistoryResponseDTO;
import com.example.backendspringboot.dto.response.RouteFromFavoritesResponseDTO;
import com.example.backendspringboot.dto.response.UserProfileResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PassengerService {
    Page<RouteFromFavoritesResponseDTO> getFavoriteRoutesForPassenger(Long passengerId, Pageable pageable);
    RouteFromFavoritesResponseDTO addRideRouteToFavorites(Long passengerId, Long rideId);
    void removeFromFavoriteRoutes(Long passengerId, Long routeId);
    List<UserProfileResponseDTO> getAllPassengers();
    List<PassengerRideHistoryResponseDTO> getPassengerRideHistory(Long passengerId);
}
