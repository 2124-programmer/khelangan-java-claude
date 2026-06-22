package com.turfbook.backend.repository;

import com.turfbook.backend.entity.EmailChangeRequestEntity;
import com.turfbook.backend.entity.EmailChangeRequestEntity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequestEntity, Long> {

    Optional<EmailChangeRequestEntity> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndStatusIn(Long userId, java.util.List<Status> statuses);

    boolean existsByNewEmailAndStatusIn(String newEmail, java.util.List<Status> statuses);

    Page<EmailChangeRequestEntity> findByStatusOrderByCreatedAtAsc(Status status, Pageable pageable);
}
