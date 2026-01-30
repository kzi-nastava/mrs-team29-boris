package com.example.demo.services.interfaces;

import com.example.demo.dto.request.RideRequestUnregisteredDTO;
import com.example.demo.dto.response.GuestRideResponseDTO;
import com.example.demo.model.GuestRide;

import java.util.List;

public interface GuestRideService {
    GuestRideResponseDTO createGuestRide(RideRequestUnregisteredDTO request);
    void cancelGuestRide(Long rideId);
    List<GuestRide> getScheduledGuestRides(Long driverId, int page, int size);
}
