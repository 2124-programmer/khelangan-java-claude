package com.turfbook.backend.repository;

import com.turfbook.backend.entity.ReviewEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    Page<ReviewEntity> findByVenueOrderByCreatedAtDesc(VenueEntity venue, Pageable pageable);

    @Query("SELECT r FROM ReviewEntity r WHERE r.venue.owner = :owner ORDER BY r.createdAt DESC")
    Page<ReviewEntity> findByVenueOwner(@Param("owner") UserEntity owner, Pageable pageable);

    Optional<ReviewEntity> findByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.venue = :venue")
    Double avgRatingByVenue(@Param("venue") VenueEntity venue);

    long countByVenue(VenueEntity venue);
}
