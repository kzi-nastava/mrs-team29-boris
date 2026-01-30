package com.example.demo.services.interfaces;

import com.example.demo.dto.response.PassengerRideHistoryResponseDTO;
import com.example.demo.dto.response.RouteFromFavoritesResponseDTO;
import com.example.demo.dto.response.UserProfileResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PassengerService {
    Page<RouteFromFavoritesResponseDTO> getFavoriteRoutesForPassenger(Long passengerId, Pageable pageable);
    void removeFromFavoriteRoutes(Long passengerId, Long routeId);
    List<UserProfileResponseDTO> getAllPassengers();
    List<PassengerRideHistoryResponseDTO> getPassengerRideHistory(Long passengerId);
}
