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

    long countByStatusIn(java.util.Collection<DisputeEntity.DisputeStatus> statuses);

    // ── Admin triage stats / party signals ──────────────────────────────────
    /** Prior-dispute counts for the party mini-cards. */
    long countByPlayer(UserEntity player);

    long countByOwner(UserEntity owner);

    /** Disputes still in an open state and older than their SLA window (overdue KPI). */
    @Query("SELECT COUNT(d) FROM DisputeEntity d WHERE d.status IN :openStatuses "
            + "AND d.raisedAt IS NOT NULL AND d.raisedAt < :cutoff")
    long countOverdue(@Param("openStatuses") java.util.Collection<DisputeEntity.DisputeStatus> openStatuses,
                      @Param("cutoff") java.time.LocalDateTime cutoff);

    long countByStatusAndResolvedAtAfter(DisputeEntity.DisputeStatus status, java.time.LocalDateTime after);

    /** Resolved-at timestamps in a window, to compute the average resolution time. */
    @Query("SELECT d.raisedAt, d.resolvedAt FROM DisputeEntity d "
            + "WHERE d.status = :status AND d.resolvedAt IS NOT NULL AND d.resolvedAt >= :after")
    java.util.List<Object[]> findResolutionDurations(
            @Param("status") DisputeEntity.DisputeStatus status,
            @Param("after") java.time.LocalDateTime after);
}
