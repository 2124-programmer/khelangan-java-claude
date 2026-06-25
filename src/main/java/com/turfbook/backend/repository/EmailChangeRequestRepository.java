package com.turfbook.backend.repository;

import com.turfbook.backend.entity.EmailChangeRequestEntity;
import com.turfbook.backend.entity.EmailChangeRequestEntity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequestEntity, Long> {

    Optional<EmailChangeRequestEntity> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    /** Clears a user's unresolved (and legacy PENDING) requests so they can always restart. */
    void deleteByUserIdAndStatusIn(Long userId, List<Status> statuses);
}
