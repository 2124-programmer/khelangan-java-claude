package com.turfbook.backend.repository;

import com.turfbook.backend.entity.ContactIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ContactIntentRepository extends JpaRepository<ContactIntentEntity, Long> {

    /**
     * True when this player already triggered an owner notification for this venue within the
     * cooldown window. Drives dedup: if true, a fresh intent is still recorded but no second
     * notification is sent.
     */
    boolean existsByPlayer_IdAndVenue_IdAndNotifiedTrueAndCreatedAtAfter(
            Long playerId, Long venueId, LocalDateTime since);
}
