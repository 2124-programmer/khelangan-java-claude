package com.turfbook.backend.repository;

import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    /** The single non-terminal ("current") subscription for a venue, if any. */
    Optional<SubscriptionEntity> findFirstByVenueAndStatusInOrderByIdDesc(
            VenueEntity venue, List<SubscriptionStatus> statuses);

    /** Full history for a venue, newest first. */
    List<SubscriptionEntity> findByVenueOrderByIdDesc(VenueEntity venue);

    /** Whether a venue has ever had any subscription (used to distinguish EXPIRED from NONE). */
    boolean existsByVenue(VenueEntity venue);

    /** An owner's subscriptions in the given statuses (across all their venues). */
    List<SubscriptionEntity> findByOwner_IdAndStatusIn(Long ownerId, List<SubscriptionStatus> statuses);

    /** Recent history for a venue, newest first (last N via Pageable). */
    List<SubscriptionEntity> findByVenue_IdOrderByIdDesc(Long venueId, Pageable pageable);

    /** Candidates for the expiry job: live-gating rows whose period has ended. */
    List<SubscriptionEntity> findByStatusInAndPeriodEndBefore(
            List<SubscriptionStatus> statuses, LocalDateTime cutoff);

    /** All rows in the given statuses (used by the expiry-reminder job for live-gating rows). */
    List<SubscriptionEntity> findByStatusIn(List<SubscriptionStatus> statuses);

    /** PAST_DUE rows whose grace window (periodEnd + grace) has elapsed. */
    List<SubscriptionEntity> findByStatus(SubscriptionStatus status);

    // ── Admin dashboard aggregates ───────────────────────────────────────────

    /** MRR: Σ price of live-gating subscriptions (TRIALING + ACTIVE). */
    @Query("SELECT COALESCE(SUM(s.price), 0) FROM SubscriptionEntity s WHERE s.status IN :statuses")
    long sumPriceByStatusIn(@Param("statuses") List<SubscriptionStatus> statuses);

    /** Count of live-gating subscriptions (the "active subscriptions" figure beside MRR). */
    long countByStatusIn(List<SubscriptionStatus> statuses);

    /** Subscriptions whose paid period ends within a window — "expiring soon". */
    long countByStatusInAndPeriodEndBetween(
            List<SubscriptionStatus> statuses, LocalDateTime from, LocalDateTime to);

    /** Trials whose trial window ends within a window — "trials ending". */
    long countByStatusAndTrialEndBetween(
            SubscriptionStatus status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT s FROM SubscriptionEntity s WHERE " +
           "(:venueId IS NULL OR s.venue.id = :venueId) AND " +
           "(:ownerId IS NULL OR s.owner.id = :ownerId) AND " +
           "(:status IS NULL OR s.status = :status) " +
           "ORDER BY s.id DESC")
    Page<SubscriptionEntity> search(
            @Param("venueId") Long venueId,
            @Param("ownerId") Long ownerId,
            @Param("status") SubscriptionStatus status,
            Pageable pageable);
}
