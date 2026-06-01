package com.turfbook.backend.repository;

import com.turfbook.backend.entity.PlatformSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformSettingsRepository extends JpaRepository<PlatformSettingsEntity, Long> {
    // Settings row always has id=1; use findById(1L)
}
