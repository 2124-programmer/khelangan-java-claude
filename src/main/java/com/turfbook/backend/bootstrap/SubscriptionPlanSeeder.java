package com.turfbook.backend.bootstrap;

import com.turfbook.backend.entity.FeatureCode;
import com.turfbook.backend.entity.PlanCode;
import com.turfbook.backend.entity.SubscriptionPlanEntity;
import com.turfbook.backend.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Idempotent seeder for the four subscription tiers. Only inserts a plan whose code is
 * missing, so admin catalog edits made later are never overwritten on restart.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanSeeder implements ApplicationRunner {

    private static final int TRIAL_DAYS = 30;

    private final SubscriptionPlanRepository planRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(PlanCode.STARTER, "Starter", 2, 499, 4990, 8, 10, 1, List.of());
        seed(PlanCode.GROWTH, "Growth", 4, 899, 8990, 12, 10, 2,
                List.of(FeatureCode.AUTO_ACCEPT, FeatureCode.OFFERS, FeatureCode.ANALYTICS));
        seed(PlanCode.PRO, "Pro", 6, 1299, 12990, 15, 30, 3,
                List.of(FeatureCode.AUTO_ACCEPT, FeatureCode.OFFERS, FeatureCode.ANALYTICS,
                        FeatureCode.PRIORITY_PLACEMENT, FeatureCode.FEATURED_BADGE));
        seed(PlanCode.PRO_MAX, "Pro Max", 12, 1999, 19990, 30, 40, 4,
                List.of(FeatureCode.AUTO_ACCEPT, FeatureCode.OFFERS, FeatureCode.ANALYTICS,
                        FeatureCode.ADVANCED_ANALYTICS, FeatureCode.PRIORITY_PLACEMENT,
                        FeatureCode.FEATURED_BADGE, FeatureCode.PRIORITY_SUPPORT));
    }

    private void seed(PlanCode code, String name, int maxCourts, int monthly, int annual,
                      int photoLimit, int placementWeight, int displayOrder, List<FeatureCode> features) {
        if (planRepository.existsByCode(code)) {
            return;
        }
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder()
                .code(code)
                .name(name)
                .maxCourts(maxCourts)
                .priceMonthly(monthly)
                .priceAnnual(annual)
                .currency("INR")
                .features(features.stream().map(Enum::name).toList())
                .photoLimit(photoLimit)
                .placementWeight(placementWeight)
                .trialDays(TRIAL_DAYS)
                .active(true)
                .displayOrder(displayOrder)
                .build();
        planRepository.save(plan);
        log.info("Seeded subscription plan {} ({})", code, name);
    }
}
