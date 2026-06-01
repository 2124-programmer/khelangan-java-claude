package com.turfbook.backend.service;

import com.turfbook.backend.dto.CreateReviewRequest;
import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.dto.ReviewPage;

public interface ReviewService {

    ReviewPage listVenueReviews(Long venueId, int page, int size);

    ReviewDto createReview(Long playerId, CreateReviewRequest request);

    ReviewPage listOwnerReviews(Long ownerId, int page, int size);
}
