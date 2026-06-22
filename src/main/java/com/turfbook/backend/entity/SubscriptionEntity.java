package com.turfbook.backend.entity;

import com.turfbook.backend.entity.converter.JsonListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A venue subscription. At most one non-terminal subscription exists per venue (the
 * "current" one); terminal rows (EXPIRED/CANCELED/VOIDED) form history.
 *
 * <p>Snapshot columns (planCode/planName/price/maxCourts/features) are copied from the
 * plan at activation/edit time so history stays accurate even if the catalog later changes.
 * All period dates are computed server-side in Asia/Kolkata — never supplied by clients.
 */
@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_sub_venue", columnList = "venue_id"),
        @Index(name = "idx_sub_owner", columnList = "owner_id"),
        @Index(name = "idx_sub_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlanEntity plan;

    // ─── Snapshots (frozen at activation/edit) ──────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, length = 20)
    private PlanCode planCode;

    @Column(name = "plan_name", nullable = false, length = 60)
    private String planName;

    @Column(nullable = false)
    private int price;

    @Column(name = "max_courts", nullable = false)
    private int maxCourts;

    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private List<String> features = new ArrayList<>();

    // ─── Lifecycle ──────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 10)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SubscriptionStatus status;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "activation_source", nullable = false, length = 20)
    @Builder.Default
    private ActivationSource activationSource = ActivationSource.ADMIN_MANUAL;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * The smallest expiry-reminder threshold (in days, e.g. 7/3/1) already notified for the current
     * period. Lets the expiry-notification job stay idempotent (never re-notify the same threshold)
     * and only escalate as the deadline nears. Reset to null whenever a fresh period starts.
     */
    @Column(name = "expiry_notified_threshold")
    private Integer expiryNotifiedThreshold;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public List<FeatureCode> getFeatureCodes() {
        List<FeatureCode> codes = new ArrayList<>();
        if (features == null) return codes;
        for (String f : features) {
            try {
                codes.add(FeatureCode.valueOf(f));
            } catch (IllegalArgumentException ignored) {
                // tolerate stale/unknown feature strings
            }
        }
        return codes;
    }
}
