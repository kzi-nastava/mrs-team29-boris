package com.example.demo.services.interfaces;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.model.Ride;
import com.example.demo.model.VehiclePrice;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RideService {
    RideResponseDTO createRide(CreateRideRequestDTO request);
//    void cancelRide(Long rideId, RideCancellationRequestDTO request);
    void panic(Long rideId);
    void startRide(Long rideId, boolean isGuest);
    void stopRide(Long rideId, RideStopRequestDTO request);
    void finishRide(Long rideId, String driverEmail, double distance, boolean isGuest);

    InconsistencyReportResponseDTO reportInconsistency(Long id,
                                                       @Valid InconsistencyReportRequestDTO dto,
                                                       String name);
    Page<ScheduledRideResponseDTO> getDriverScheduledRides(Long driverId, int page, int size);
    void cancelAnyRide(Long rideId, RideCancellationRequestDTO request);
    PassengerRideDetailsResponseDTO getRideDetails(Long rideId);

    RideTrackingResponseDTO getRideTracking(Long rideId);

}
