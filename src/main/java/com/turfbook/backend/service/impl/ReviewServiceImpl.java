package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.CreateReviewRequest;
import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.dto.ReviewPage;
import com.turfbook.backend.dto.UpdateReviewRequest;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.ReviewEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.mapper.ReviewMapper;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.ReviewRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public ReviewPage listVenueReviews(Long venueId, Long callerId, int page, int size) {
        VenueEntity venue = requireVenue(venueId);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewEntity> entityPage = reviewRepository.findByVenueOrderByCreatedAtDesc(venue, pageable);
        return toReviewPage(entityPage, callerId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDto getMyVenueReview(Long venueId, Long playerId) {
        VenueEntity venue = requireVenue(venueId);
        UserEntity player = requireUser(playerId);
        return reviewRepository.findByVenueAndPlayer(venue, player)
                .map(r -> reviewMapper.toDto(r, true, null))
                .orElse(null);
    }

    @Override
    @Transactional
    public ReviewDto createReview(Long playerId, Long venueId, CreateReviewRequest request) {
        UserEntity player = requireUser(playerId);
        VenueEntity venue = requireVenue(venueId);

        List<BookingEntity.BookingStatus> doneStatuses =
                List.of(BookingEntity.BookingStatus.COMPLETED, BookingEntity.BookingStatus.CHECKED_IN);
        if (!bookingRepository.existsByPlayerAndVenueAndStatusIn(player, venue, doneStatuses)) {
            throw new ForbiddenException("You must have a completed booking at this venue to leave a review");
        }
        if (reviewRepository.existsByVenueAndPlayer(venue, player)) {
            throw new ConflictException("You have already reviewed this venue. Use PUT to edit your review.");
        }

        ReviewEntity review = ReviewEntity.builder()
                .venue(venue)
                .player(player)
                .authorName(player.getName())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        recomputeVenueAggregates(venue);

        log.info("Review created - reviewId={}, venueId={}, playerId={}", review.getId(), venueId, playerId);
        return reviewMapper.toDto(review, true, null);
    }

    @Override
    @Transactional
    public ReviewDto updateReview(Long reviewId, Long playerId, UpdateReviewRequest request) {
        ReviewEntity review = requireReview(reviewId);
        if (!review.getPlayer().getId().equals(playerId)) {
            throw new ForbiddenException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());
        review = reviewRepository.save(review);
        recomputeVenueAggregates(review.getVenue());

        log.info("Review updated - reviewId={}, playerId={}", reviewId, playerId);
        return reviewMapper.toDto(review, true, null);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long playerId) {
        ReviewEntity review = requireReview(reviewId);
        if (!review.getPlayer().getId().equals(playerId)) {
            throw new ForbiddenException("You can only delete your own reviews");
        }

        VenueEntity venue = review.getVenue();
        reviewRepository.delete(review);
        recomputeVenueAggregates(venue);

        log.info("Review deleted - reviewId={}, playerId={}", reviewId, playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPage listOwnerReviews(Long ownerId, int page, int size) {
        UserEntity owner = requireUser(ownerId);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewEntity> entityPage = reviewRepository.findByVenueOwner(owner, pageable);
        return toOwnerReviewPage(entityPage);
    }

    private void recomputeVenueAggregates(VenueEntity venue) {
        long count = reviewRepository.countByVenue(venue);
        Double avg = count > 0 ? reviewRepository.avgRatingByVenue(venue) : null;
        venue.setRatingCount(count);
        venue.setRatingAverage(avg);
        venueRepository.save(venue);
    }

    private VenueEntity requireVenue(Long venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private ReviewEntity requireReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    }

    private ReviewPage toReviewPage(Page<ReviewEntity> entityPage, Long callerId) {
        ReviewPage dto = new ReviewPage();
        dto.setContent(entityPage.getContent().stream()
                .map(r -> reviewMapper.toDto(r, callerId != null && r.getPlayer().getId().equals(callerId), null))
                .toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }

    private ReviewPage toOwnerReviewPage(Page<ReviewEntity> entityPage) {
        ReviewPage dto = new ReviewPage();
        dto.setContent(entityPage.getContent().stream()
                .map(r -> reviewMapper.toDto(r, false, r.getVenue().getName()))
                .toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }
}
