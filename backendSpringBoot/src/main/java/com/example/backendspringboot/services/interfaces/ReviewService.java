package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.request.ReviewRequestDTO;

import java.util.List;

public interface ReviewService {
    List<ReviewRequestDTO> getAll(long rideId);
    void createReview(ReviewRequestDTO dto);
}
