package com.turfbook.backend.repository;

import com.turfbook.backend.entity.PhoneChangeRequestEntity;
import com.turfbook.backend.entity.PhoneChangeRequestEntity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneChangeRequestRepository extends JpaRepository<PhoneChangeRequestEntity, Long> {

    Optional<PhoneChangeRequestEntity> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    /** Clears a user's unresolved requests so they can always restart. */
    void deleteByUserIdAndStatusIn(Long userId, List<Status> statuses);
}
