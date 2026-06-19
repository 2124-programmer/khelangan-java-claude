package com.turfbook.backend.repository;

import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.CourtEntity;
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
import java.util.Collection;
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

    // Owner: bookings for their venues (no date filter – legacy path)
    @Query("SELECT b FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND (:status IS NULL OR b.status = :status) ORDER BY b.createdAt DESC")
    Page<BookingEntity> findByVenueOwner(
            @Param("owner") UserEntity owner,
            @Param("status") BookingEntity.BookingStatus status,
            Pageable pageable
    );

    // Owner: bookings with optional exact-date and date-range filtering
    // date     → exact match (Today tab)
    // dateFrom → b.date >= dateFrom (Upcoming tab)
    // Both nullable; when both are null this behaves like findByVenueOwner
    @Query("SELECT b FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:date IS NULL OR b.date = :date) " +
           "AND (:dateFrom IS NULL OR b.date >= :dateFrom) " +
           "ORDER BY b.date ASC, b.startTime ASC")
    Page<BookingEntity> findByVenueOwnerWithDateFilter(
            @Param("owner") UserEntity owner,
            @Param("status") BookingEntity.BookingStatus status,
            @Param("date") LocalDate date,
            @Param("dateFrom") LocalDate dateFrom,
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

    // For slot status resolution in listSlots
    List<BookingEntity> findByCourtAndDateAndStatusIn(
            CourtEntity court,
            LocalDate date,
            Collection<BookingEntity.BookingStatus> statuses
    );

    // Dashboard summary: bookings whose slot date = given date and status in the provided set
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND b.date = :date AND b.status IN :statuses")
    long countByOwnerAndDateAndStatusIn(
            @Param("owner") UserEntity owner,
            @Param("date") LocalDate date,
            @Param("statuses") Collection<BookingEntity.BookingStatus> statuses
    );

    // Dashboard summary: CONFIRMED bookings with slot date strictly after :after
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND b.date > :after AND b.status = :status")
    long countByOwnerAndDateAfterAndStatus(
            @Param("owner") UserEntity owner,
            @Param("after") LocalDate after,
            @Param("status") BookingEntity.BookingStatus status
    );

    // Dashboard summary: bookings with status = :status and slot date in [:from, :to]
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND b.status = :status AND b.date >= :from AND b.date <= :to")
    long countByOwnerAndStatusAndSlotDateBetween(
            @Param("owner") UserEntity owner,
            @Param("status") BookingEntity.BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    // Dashboard summary: bookings with status IN :statuses and slot date in [:from, :to]
    // Used for "Done" card which mirrors the Completed tab (COMPLETED + CHECKED_IN)
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND b.status IN :statuses AND b.date >= :from AND b.date <= :to")
    long countByOwnerAndStatusInAndSlotDateBetween(
            @Param("owner") UserEntity owner,
            @Param("statuses") Collection<BookingEntity.BookingStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    // Dashboard summary: cancelled/rejected bookings created within the window
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.venue.owner = :owner " +
           "AND b.status IN :statuses AND b.createdAt >= :from")
    long countByOwnerAndStatusInAndCreatedAtAfter(
            @Param("owner") UserEntity owner,
            @Param("statuses") Collection<BookingEntity.BookingStatus> statuses,
            @Param("from") LocalDateTime from
    );

    // Dashboard summary: pending booking requests awaiting owner action
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.venue.owner = :owner AND b.status = :status")
    long countByOwnerAndStatus(@Param("owner") UserEntity owner,
                               @Param("status") BookingEntity.BookingStatus status);

    // Dashboard summary: distinct customers who have ever booked this owner's venues
    @Query("SELECT COUNT(DISTINCT b.player) FROM BookingEntity b WHERE b.venue.owner = :owner")
    long countDistinctPlayersByOwner(@Param("owner") UserEntity owner);

    // Group booking lookup
    List<BookingEntity> findByGroupIdOrderByStartTimeAsc(String groupId);

    // PENDING bookings whose 24-hour acceptance window has passed.
    // LEFT JOIN FETCH avoids lazy proxy initialization for slots that may have been deleted.
    @Query("SELECT b FROM BookingEntity b LEFT JOIN FETCH b.slot WHERE b.status = 'PENDING' AND b.createdAt < :expiredBefore")
    List<BookingEntity> findExpiredPendingBookings(@Param("expiredBefore") LocalDateTime expiredBefore);
}
