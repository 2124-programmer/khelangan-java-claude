package com.turfbook.backend.controller;

import com.turfbook.backend.api.ReviewsApi;
import com.turfbook.backend.dto.CreateReviewRequest;
import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.dto.ReviewPage;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewsApi {

    private final ReviewService reviewService;

    @Override
    public ResponseEntity<ReviewPage> listVenueReviews(Long venueId, Integer page, Integer size) {
        return ResponseEntity.ok(reviewService.listVenueReviews(venueId,
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ReviewDto> createReview(CreateReviewRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReviewPage> listOwnerReviews(Integer page, Integer size) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(reviewService.listOwnerReviews(principal.getId(),
                page != null ? page : 0, size != null ? size : 20));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
