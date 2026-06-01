package com.turfbook.backend.repository;

import com.turfbook.backend.entity.SportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SportRepository extends JpaRepository<SportEntity, Long> {

    Optional<SportEntity> findByName(String name);

    boolean existsByName(String name);
}
