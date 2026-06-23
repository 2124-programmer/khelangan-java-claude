package com.turfbook.backend.repository;

import com.turfbook.backend.entity.DisputeEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeEventRepository extends JpaRepository<DisputeEventEntity, Long> {
    List<DisputeEventEntity> findByDispute_IdOrderByCreatedAtAsc(Long disputeId);
}
