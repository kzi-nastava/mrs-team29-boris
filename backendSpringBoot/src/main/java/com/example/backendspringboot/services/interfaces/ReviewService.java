package com.example.demo.services.interfaces;

import com.example.demo.dto.request.ReviewRequestDTO;

import java.util.List;

public interface ReviewService {
    List<ReviewRequestDTO> getAll(long rideId);
    void createReview(ReviewRequestDTO dto);
}
