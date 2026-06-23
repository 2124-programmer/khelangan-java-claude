package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only audit trail of sensitive admin actions taken against a target user (player/owner).
 * Never updated or deleted — one row per moderation action.
 */
@Entity
@Table(name = "admin_audit", indexes = {
        @Index(name = "idx_audit_target", columnList = "target_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The admin who performed the action. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserEntity actor;

    /** The user the action targeted. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private UserEntity target;

    /** Action code (SUSPEND, BAN, REACTIVATE, …). */
    @Column(nullable = false, length = 32)
    private String action;

    @Column(length = 500)
    private String reason;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", length = 20)
    private String toStatus;

    @Column(length = 500)
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
