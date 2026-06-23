package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An append-only audit entry for a venue's lifecycle journey
 * (Registered → Approved → Trial activated → Subscription, plus renew/change/suspend).
 * The admin subscription timeline is computed from concrete venue/subscription fields;
 * this log is the durable, extensible record of how the venue got there.
 */
@Entity
@Table(name = "venue_lifecycle_events", indexes = {
        @Index(name = "idx_vle_venue", columnList = "venue_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueLifecycleEventEntity {

    public enum Type {
        REGISTERED,
        APPROVED,
        TRIAL_ACTIVATED,
        SUBSCRIPTION_STARTED,
        SUBSCRIPTION_RENEWED,
        SUBSCRIPTION_CHANGED,
        SUSPENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Type type;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Optional human-readable context (e.g. plan name). */
    @Column(length = 300)
    private String meta;
}
