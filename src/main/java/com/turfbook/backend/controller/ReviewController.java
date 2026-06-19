package com.turfbook.backend.controller;

import com.turfbook.backend.api.ReviewsApi;
import com.turfbook.backend.dto.CreateReviewRequest;
import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.dto.ReviewPage;
import com.turfbook.backend.dto.UpdateReviewRequest;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewsApi {

    private final ReviewService reviewService;

    @Override
    public ResponseEntity<ReviewPage> listVenueReviews(Long venueId, Integer page, Integer size) {
        Long callerId = getOptionalPrincipalId();
        return ResponseEntity.ok(reviewService.listVenueReviews(venueId, callerId,
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ReviewDto> getMyVenueReview(Long venueId) {
        Long playerId = getPrincipalId();
        ReviewDto dto = reviewService.getMyVenueReview(venueId, playerId);
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ReviewDto> createVenueReview(Long venueId, CreateReviewRequest createReviewRequest) {
        Long playerId = getPrincipalId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(playerId, venueId, createReviewRequest));
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ReviewDto> updateReview(Long reviewId, UpdateReviewRequest updateReviewRequest) {
        Long playerId = getPrincipalId();
        return ResponseEntity.ok(reviewService.updateReview(reviewId, playerId, updateReviewRequest));
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<Void> deleteReview(Long reviewId) {
        Long playerId = getPrincipalId();
        reviewService.deleteReview(reviewId, playerId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReviewPage> listOwnerReviews(Integer page, Integer size) {
        Long ownerId = getPrincipalId();
        return ResponseEntity.ok(reviewService.listOwnerReviews(ownerId,
                page != null ? page : 0, size != null ? size : 20));
    }

    private Long getPrincipalId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    private Long getOptionalPrincipalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        return null;
    }
}
