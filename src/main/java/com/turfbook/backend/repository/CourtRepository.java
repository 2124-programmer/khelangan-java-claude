package com.turfbook.backend.repository;

import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourtRepository extends JpaRepository<CourtEntity, Long> {

    List<CourtEntity> findByVenue(VenueEntity venue);

    Optional<CourtEntity> findByIdAndVenue(Long id, VenueEntity venue);
}
