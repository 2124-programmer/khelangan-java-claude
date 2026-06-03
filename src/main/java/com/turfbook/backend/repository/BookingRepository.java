package com.turfbook.backend.repository;

import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    // Player: own bookings
    Page<BookingEntity> findByPlayerOrderByCreatedAtDesc(UserEntity player, Pageable pageable);

    Page<BookingEntity> findByPlayerAndStatusOrderByCreatedAtDesc(
            UserEntity player,
            BookingEntity.BookingStatus status,
            Pageable pageable
    );

    // Owner: bookings for their venues
    @Query("SELECT b FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND (:status IS NULL OR b.status = :status) ORDER BY b.createdAt DESC")
    Page<BookingEntity> findByVenueOwner(
            @Param("owner") UserEntity owner,
            @Param("status") BookingEntity.BookingStatus status,
            Pageable pageable
    );

    // Admin: all bookings
    @Query("SELECT b FROM BookingEntity b WHERE (:status IS NULL OR b.status = :status) ORDER BY b.createdAt DESC")
    Page<BookingEntity> findAllByStatus(
            @Param("status") BookingEntity.BookingStatus status,
            Pageable pageable
    );

    // Stats
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.createdAt >= :from AND b.createdAt < :to")
    long countTodayBookings(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BookingEntity b " +
           "WHERE b.createdAt >= :from AND b.createdAt < :to AND b.paymentStatus = :successStatus")
    long sumRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                    @Param("successStatus") BookingEntity.PaymentStatus successStatus);

    // Owner stats
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND b.createdAt >= :from AND b.createdAt < :to")
    long countByOwnerAndDateRange(
            @Param("owner") UserEntity owner,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND b.createdAt >= :from AND b.createdAt < :to AND b.paymentStatus = :successStatus")
    long sumRevenueByOwnerAndDateRange(
            @Param("owner") UserEntity owner,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("successStatus") BookingEntity.PaymentStatus successStatus
    );

    // For a specific venue
    List<BookingEntity> findByVenueAndDateAndStatus(
            VenueEntity venue,
            LocalDate date,
            BookingEntity.BookingStatus status
    );

    // PENDING bookings whose 24-hour acceptance window has passed
    @Query("SELECT b FROM BookingEntity b WHERE b.status = 'PENDING' AND b.createdAt < :expiredBefore")
    List<BookingEntity> findExpiredPendingBookings(@Param("expiredBefore") LocalDateTime expiredBefore);
}
