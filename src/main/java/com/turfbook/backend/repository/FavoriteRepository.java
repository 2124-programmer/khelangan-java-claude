package com.turfbook.backend.repository;

import com.turfbook.backend.entity.FavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    boolean existsByVenue_IdAndPlayer_Id(Long venueId, Long playerId);

    Optional<FavoriteEntity> findByVenue_IdAndPlayer_Id(Long venueId, Long playerId);

    /** Venue ids the player has favorited — used to flag a page of venue summaries in one query. */
    @Query("SELECT f.venue.id FROM FavoriteEntity f WHERE f.player.id = :playerId")
    Set<Long> findVenueIdsByPlayerId(@Param("playerId") Long playerId);
}
