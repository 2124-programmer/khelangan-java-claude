package com.turfbook.backend.repository;

import com.turfbook.backend.entity.CourtChangeRequestEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourtChangeRequestRepository
        extends JpaRepository<CourtChangeRequestEntity, Long> {

    long countByStatus(CourtChangeRequestEntity.Status status);

    boolean existsByVenueAndStatus(VenueEntity venue, CourtChangeRequestEntity.Status status);

    Optional<CourtChangeRequestEntity> findFirstByVenueAndStatusOrderByCreatedAtDesc(
            VenueEntity venue, CourtChangeRequestEntity.Status status);

    List<CourtChangeRequestEntity> findByOwnerOrderByCreatedAtDesc(UserEntity owner);

    /**
     * Super-admin queue: eager-fetch venue + owner so the DTO mapper never trips a lazy proxy at
     * serialization, and INNER JOINs skip requests whose venue/owner row was since deleted.
     */
    @Query("SELECT c FROM CourtChangeRequestEntity c "
            + "JOIN FETCH c.venue "
            + "JOIN FETCH c.owner "
            + "WHERE c.status = :status ORDER BY c.createdAt ASC")
    List<CourtChangeRequestEntity> findByStatusWithRefs(
            @Param("status") CourtChangeRequestEntity.Status status);
}
