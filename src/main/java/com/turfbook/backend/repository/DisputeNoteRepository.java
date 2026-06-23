package com.turfbook.backend.repository;

import com.turfbook.backend.entity.DisputeNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeNoteRepository extends JpaRepository<DisputeNoteEntity, Long> {
    List<DisputeNoteEntity> findByDispute_IdOrderByCreatedAtAsc(Long disputeId);
}
