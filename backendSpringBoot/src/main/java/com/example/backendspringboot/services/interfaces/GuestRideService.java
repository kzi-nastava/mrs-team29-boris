package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.request.RideRequestUnregisteredDTO;
import com.example.backendspringboot.dto.response.GuestRideResponseDTO;
import com.example.backendspringboot.model.GuestRide;

import java.util.List;

public interface GuestRideService {
    GuestRideResponseDTO createGuestRide(RideRequestUnregisteredDTO request);
    void cancelGuestRide(Long rideId);
    List<GuestRide> getScheduledGuestRides(Long driverId, int page, int size);
}
