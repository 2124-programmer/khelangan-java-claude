package com.turfbook.backend.repository;

import com.turfbook.backend.entity.DisputeEntity;
import com.turfbook.backend.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<DisputeEntity, Long> {

    Page<DisputeEntity> findByPlayerOrderByCreatedAtDesc(UserEntity player, Pageable pageable);

    Page<DisputeEntity> findByOwnerOrderByCreatedAtDesc(UserEntity owner, Pageable pageable);

    @Query("SELECT d FROM DisputeEntity d ORDER BY d.createdAt DESC")
    Page<DisputeEntity> findAllOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(DisputeEntity.DisputeStatus status);
}
