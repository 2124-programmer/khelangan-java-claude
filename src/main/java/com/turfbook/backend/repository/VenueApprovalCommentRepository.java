package com.turfbook.backend.repository;

import com.turfbook.backend.entity.VenueApprovalCommentEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueApprovalCommentRepository extends JpaRepository<VenueApprovalCommentEntity, Long> {

    List<VenueApprovalCommentEntity> findByVenueOrderByCreatedAtAsc(VenueEntity venue);
}
