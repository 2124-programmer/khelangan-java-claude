package com.turfbook.backend.entity;

import com.turfbook.backend.entity.converter.JsonListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalog of subscription tiers. Seeded with the four plans (STARTER/GROWTH/PRO/PRO_MAX)
 * and editable by admin so prices/limits/features are tunable without code.
 */
@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private PlanCode code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "max_courts", nullable = false)
    private int maxCourts;

    @Column(name = "price_monthly", nullable = false)
    private int priceMonthly;

    @Column(name = "price_annual", nullable = false)
    private int priceAnnual;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    /** FeatureCode names (JSON). Use {@link #getFeatureCodes()} for typed access. */
    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private List<String> features = new ArrayList<>();

    @Column(name = "photo_limit", nullable = false)
    private int photoLimit;

    @Column(name = "placement_weight", nullable = false)
    @Builder.Default
    private int placementWeight = 10;

    @Column(name = "trial_days", nullable = false)
    @Builder.Default
    private int trialDays = 30;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Transient
    public List<FeatureCode> getFeatureCodes() {
        List<FeatureCode> codes = new ArrayList<>();
        if (features == null) return codes;
        for (String f : features) {
            try {
                codes.add(FeatureCode.valueOf(f));
            } catch (IllegalArgumentException ignored) {
                // tolerate stale/unknown feature strings in stored JSON
            }
        }
        return codes;
    }
}
