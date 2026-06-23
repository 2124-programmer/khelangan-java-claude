package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Append-only timeline/audit entry for a dispute (raised, assigned, messaged, resolved, …). */
@Entity
@Table(name = "dispute_events", indexes = @Index(name = "idx_dispute_event", columnList = "dispute_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispute_id", nullable = false)
    private DisputeEntity dispute;

    /** A DisputeActionCode value (ASSIGN/MESSAGE/REQUEST_INFO/ADD_NOTE/RESOLVE/DISMISS/REOPEN/APPLY_CONSEQUENCE). */
    @Column(nullable = false, length = 24)
    private String action;

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(length = 500)
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
