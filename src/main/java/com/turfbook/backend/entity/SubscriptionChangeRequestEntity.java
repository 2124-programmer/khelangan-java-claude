package com.turfbook.backend.entity;

import com.turfbook.backend.entity.converter.JsonListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An owner-initiated upgrade/plan-change request. Admin activates it (applying the
 * requested plan as a new active subscription) or rejects it with a reason.
 */
@Entity
@Table(name = "subscription_change_requests", indexes = {
        @Index(name = "idx_scr_venue", columnList = "venue_id"),
        @Index(name = "idx_scr_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionChangeRequestEntity {

    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    /** The subscription in effect when the request was made (may be null if none). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_subscription_id")
    private SubscriptionEntity currentSubscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_plan_id", nullable = false)
    private SubscriptionPlanEntity requestedPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_cycle", nullable = false, length = 10)
    private BillingCycle requestedCycle;

    /**
     * Courts the owner selected to cover at request time (court ids as strings, count ≤ the
     * requested plan's maxCourts). Applied onto the activated subscription's coveredCourtIds
     * when an admin approves the request — so the owner's court choice survives the approval gap.
     */
    @Convert(converter = JsonListConverter.class)
    @Column(name = "covered_court_ids", columnDefinition = "json")
    @Builder.Default
    private List<String> coveredCourtIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(length = 500)
    private String reason;
}
