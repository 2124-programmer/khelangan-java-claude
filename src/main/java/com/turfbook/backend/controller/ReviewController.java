package com.turfbook.backend.controller;

import com.turfbook.backend.api.ReviewsApi;
import com.turfbook.backend.dto.CreateReviewRequest;
import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.dto.ReviewPage;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewsApi {

    private final ReviewService reviewService;

    @Override
    public ResponseEntity<ReviewPage> listVenueReviews(Long venueId, Integer page, Integer size) {
        log.info("ReviewController.listVenueReviews() called - venueId={}", venueId);
        return ResponseEntity.ok(reviewService.listVenueReviews(venueId,
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ReviewDto> createReview(CreateReviewRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("ReviewController.createReview() called - userId={}, bookingId={}", principal.getId(), request.getBookingId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReviewPage> listOwnerReviews(Integer page, Integer size) {
        UserPrincipal principal = getPrincipal();
        log.info("ReviewController.listOwnerReviews() called - ownerId={}", principal.getId());
        return ResponseEntity.ok(reviewService.listOwnerReviews(principal.getId(),
                page != null ? page : 0, size != null ? size : 20));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
