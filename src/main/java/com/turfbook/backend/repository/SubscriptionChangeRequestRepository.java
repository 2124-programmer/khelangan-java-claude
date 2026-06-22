package com.turfbook.backend.repository;

import com.turfbook.backend.entity.SubscriptionChangeRequestEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionChangeRequestRepository
        extends JpaRepository<SubscriptionChangeRequestEntity, Long> {

    List<SubscriptionChangeRequestEntity> findByStatusOrderByCreatedAtAsc(
            SubscriptionChangeRequestEntity.Status status);

    Optional<SubscriptionChangeRequestEntity> findFirstByVenueAndStatusOrderByCreatedAtDesc(
            VenueEntity venue, SubscriptionChangeRequestEntity.Status status);

    boolean existsByVenueAndStatus(VenueEntity venue, SubscriptionChangeRequestEntity.Status status);
}
