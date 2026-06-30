package com.turfbook.backend.repository;

import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.CourtStatus;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourtRepository extends JpaRepository<CourtEntity, Long> {

    List<CourtEntity> findByVenue(VenueEntity venue);

    long countByVenue(VenueEntity venue);

    /** Court count excluding soft-deleted rows (pass {@code CourtStatus.ACTIVE}). */
    long countByVenueAndStatus(VenueEntity venue, CourtStatus status);

    Optional<CourtEntity> findByIdAndVenue(Long id, VenueEntity venue);

    boolean existsByVenueAndName(VenueEntity venue, String name);

    boolean existsByVenueAndNameAndIdNot(VenueEntity venue, String name, Long id);

    /** Name-uniqueness check that ignores soft-deleted rows (pass {@code CourtStatus.ACTIVE}). */
    boolean existsByVenueAndNameAndStatus(VenueEntity venue, String name, CourtStatus status);

    boolean existsByVenueAndNameAndIdNotAndStatus(VenueEntity venue, String name, Long id, CourtStatus status);

    /** Owner-wide court count, excluding soft-deleted rows. */
    @Query("SELECT COUNT(c) FROM CourtEntity c WHERE c.venue.owner = :owner AND c.status <> 'DELETED'")
    long countByVenueOwner(@Param("owner") UserEntity owner);
}
