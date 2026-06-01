package com.turfbook.backend.repository;

import com.turfbook.backend.entity.PayoutEntity;
import com.turfbook.backend.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutRepository extends JpaRepository<PayoutEntity, Long> {

    Page<PayoutEntity> findByOwnerOrderByCreatedAtDesc(UserEntity owner, Pageable pageable);

    @Query("SELECT p FROM PayoutEntity p WHERE (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    Page<PayoutEntity> findAllByStatus(
            @Param("status") PayoutEntity.PayoutStatus status,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(p.netAmount), 0) FROM PayoutEntity p WHERE p.owner = :owner AND p.status = :pendingStatus")
    long sumPendingByOwner(@Param("owner") UserEntity owner,
                           @Param("pendingStatus") PayoutEntity.PayoutStatus pendingStatus);
}
