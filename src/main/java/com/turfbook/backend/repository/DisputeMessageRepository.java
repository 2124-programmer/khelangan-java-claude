package com.turfbook.backend.repository;

import com.turfbook.backend.entity.DisputeMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeMessageRepository extends JpaRepository<DisputeMessageEntity, Long> {
    List<DisputeMessageEntity> findByDispute_IdOrderByCreatedAtAsc(Long disputeId);
}
