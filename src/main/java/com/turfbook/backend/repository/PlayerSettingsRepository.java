package com.turfbook.backend.repository;

import com.turfbook.backend.entity.PlayerSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerSettingsRepository extends JpaRepository<PlayerSettingsEntity, Long> {

    Optional<PlayerSettingsEntity> findByPlayerId(Long playerId);
}
