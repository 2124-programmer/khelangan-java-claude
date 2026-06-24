package com.turfbook.backend.repository;

import com.turfbook.backend.entity.SubscriptionChangeRequestEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionChangeRequestRepository
        extends JpaRepository<SubscriptionChangeRequestEntity, Long> {

    List<SubscriptionChangeRequestEntity> findByStatusOrderByCreatedAtAsc(
            SubscriptionChangeRequestEntity.Status status);

    /**
     * Admin listing: eager-fetch the references the DTO mapper reads so it never trips a lazy
     * proxy at serialization time. The INNER JOINs on venue/owner/requestedPlan also naturally
     * skip orphaned requests whose venue/owner/plan row has since been deleted (instead of 500ing).
     */
    @Query("SELECT s FROM SubscriptionChangeRequestEntity s "
            + "JOIN FETCH s.venue "
            + "JOIN FETCH s.owner "
            + "JOIN FETCH s.requestedPlan "
            + "LEFT JOIN FETCH s.currentSubscription "
            + "WHERE s.status = :status ORDER BY s.createdAt DESC")
    List<SubscriptionChangeRequestEntity> findByStatusWithRefs(
            @Param("status") SubscriptionChangeRequestEntity.Status status);

    Optional<SubscriptionChangeRequestEntity> findFirstByVenueAndStatusOrderByCreatedAtDesc(
            VenueEntity venue, SubscriptionChangeRequestEntity.Status status);

    boolean existsByVenueAndStatus(VenueEntity venue, SubscriptionChangeRequestEntity.Status status);
}
