package com.turfbook.backend.repository;

import com.turfbook.backend.entity.OwnerSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerSettingsRepository extends JpaRepository<OwnerSettingsEntity, Long> {

    Optional<OwnerSettingsEntity> findByOwnerId(Long ownerId);
}
