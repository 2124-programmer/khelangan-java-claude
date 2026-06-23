package com.turfbook.backend.repository;

import com.turfbook.backend.entity.AdminAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditRepository extends JpaRepository<AdminAuditEntity, Long> {

    /** Audit feed for one target user, newest first; actor eager-fetched for display. */
    @Query("SELECT a FROM AdminAuditEntity a LEFT JOIN FETCH a.actor "
            + "WHERE a.target.id = :targetId ORDER BY a.createdAt DESC")
    Page<AdminAuditEntity> findByTargetId(@Param("targetId") Long targetId, Pageable pageable);
}
