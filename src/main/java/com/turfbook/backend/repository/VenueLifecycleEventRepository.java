package com.turfbook.backend.repository;

import com.turfbook.backend.entity.VenueLifecycleEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueLifecycleEventRepository extends JpaRepository<VenueLifecycleEventEntity, Long> {

    /** A venue's lifecycle audit trail, oldest first. */
    List<VenueLifecycleEventEntity> findByVenue_IdOrderByOccurredAtAsc(Long venueId);
}
