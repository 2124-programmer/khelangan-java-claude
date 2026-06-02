package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.CreateReviewRequest;
import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.dto.ReviewPage;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.ReviewEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.UnauthorizedException;
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
    public ReviewPage listVenueReviews(Long venueId, int page, int size) {
        log.info("ReviewService.listVenueReviews() called - venueId={}", venueId);
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewEntity> entityPage = reviewRepository.findByVenueOrderByCreatedAtDesc(venue, pageable);
        return toReviewPage(entityPage);
    }

    @Override
    @Transactional
    public ReviewDto createReview(Long playerId, CreateReviewRequest request) {
        log.info("ReviewService.createReview() called - playerId={}, bookingId={}, rating={}",
                playerId, request.getBookingId(), request.getRating());
        UserEntity player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", playerId));

        BookingEntity booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        if (!booking.getPlayer().getId().equals(playerId)) {
            throw new UnauthorizedException("You can only review your own bookings");
        }
        if (booking.getStatus() != BookingEntity.BookingStatus.COMPLETED) {
            throw new ConflictException("You can only review completed bookings");
        }
        if (booking.isHasReview()) {
            throw new ConflictException("You have already submitted a review for this booking");
        }

        ReviewEntity review = ReviewEntity.builder()
                .booking(booking)
                .venue(booking.getVenue())
                .player(player)
                .playerName(player.getName())
                .rating(request.getRating())
                .comment(request.getComment())
                .cleanliness(request.getCleanliness())
                .ground(request.getGround())
                .staff(request.getStaff())
                .build();

        review = reviewRepository.save(review);

        booking.setHasReview(true);
        bookingRepository.save(booking);

        VenueEntity venue = booking.getVenue();
        Double avgRating = reviewRepository.avgRatingByVenue(venue);
        long reviewCount = reviewRepository.countByVenue(venue);
        venue.setRating(avgRating != null ? avgRating : 0.0);
        venue.setReviewCount((int) reviewCount);
        venueRepository.save(venue);

        log.info("ReviewService.createReview() completed - reviewId={}, venueId={}", review.getId(), venue.getId());
        return reviewMapper.toDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPage listOwnerReviews(Long ownerId, int page, int size) {
        log.info("ReviewService.listOwnerReviews() called - ownerId={}", ownerId);
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewEntity> entityPage = reviewRepository.findByVenueOwner(owner, pageable);
        return toReviewPage(entityPage);
    }

    private ReviewPage toReviewPage(Page<ReviewEntity> entityPage) {
        ReviewPage dto = new ReviewPage();
        dto.setContent(entityPage.getContent().stream().map(reviewMapper::toDto).toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }
}
