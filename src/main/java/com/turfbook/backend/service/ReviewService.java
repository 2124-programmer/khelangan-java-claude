package com.turfbook.backend.service;

import com.turfbook.backend.dto.CreateReviewRequest;
import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.dto.ReviewPage;
import com.turfbook.backend.dto.UpdateReviewRequest;

public interface ReviewService {

    ReviewPage listVenueReviews(Long venueId, Long callerId, int page, int size);

    ReviewDto getMyVenueReview(Long venueId, Long playerId);

    ReviewDto createReview(Long playerId, Long venueId, CreateReviewRequest request);

    ReviewDto updateReview(Long reviewId, Long playerId, UpdateReviewRequest request);

    void deleteReview(Long reviewId, Long playerId);

    ReviewPage listOwnerReviews(Long ownerId, int page, int size);
}
