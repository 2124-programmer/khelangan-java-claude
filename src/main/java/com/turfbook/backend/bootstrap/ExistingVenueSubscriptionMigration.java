package com.turfbook.backend.bootstrap;

import com.turfbook.backend.entity.ActivationSource;
import com.turfbook.backend.entity.BillingCycle;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionPlanEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.SubscriptionPlanRepository;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.subscription.SubscriptionDateCalculator;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rollout migration: every venue that is already LIVE but has no subscription would
 * silently disappear once the subscription gate is enforced. To prevent that, we grant
 * each such venue a comp 30-day TRIAL on the smallest plan that fits its current court
 * count, and flip its denormalized live flag on. Idempotent — venues that already have a
 * subscription (current or historical) are skipped, so it is safe on every restart.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ExistingVenueSubscriptionMigration implements ApplicationRunner {

    private static final List<SubscriptionStatus> ANY_CURRENT =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final VenueRepository venueRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final CourtRepository courtRepository;
    private final SubscriptionDateCalculator dates;
    private final SubscriptionGate subscriptionGate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SubscriptionPlanEntity> plans = planRepository.findAllByOrderByDisplayOrderAscIdAsc();
        if (plans.isEmpty()) {
            log.warn("No subscription plans seeded; skipping existing-venue migration.");
            return;
        }

        int granted = 0;
        for (VenueEntity venue : venueRepository.findAll()) {
            if (venue.getStatus() != VenueEntity.VenueStatus.LIVE) {
                continue;
            }
            // Skip venues that already have any non-terminal subscription.
            if (subscriptionRepository.findFirstByVenueAndStatusInOrderByIdDesc(venue, ANY_CURRENT).isPresent()) {
                continue;
            }
            // Also skip if a comp trial was already granted in a prior run (any history exists).
            if (!subscriptionRepository.findByVenueOrderByIdDesc(venue).isEmpty()) {
                continue;
            }

            long courts = courtRepository.countByVenue(venue);
            SubscriptionPlanEntity plan = smallestPlanFor(plans, courts);

            LocalDateTime now = dates.now();
            LocalDateTime trialEnd = dates.trialEnd(now, plan.getTrialDays());
            SubscriptionEntity sub = SubscriptionEntity.builder()
                    .owner(venue.getOwner())
                    .venue(venue)
                    .plan(plan)
                    .planCode(plan.getCode())
                    .planName(plan.getName())
                    .price(plan.getPriceMonthly())
                    .maxCourts(plan.getMaxCourts())
                    .features(new ArrayList<>(plan.getFeatures()))
                    .billingCycle(BillingCycle.MONTHLY)
                    .status(SubscriptionStatus.TRIALING)
                    .periodStart(now)
                    .periodEnd(trialEnd)
                    .trialEnd(trialEnd)
                    .currency(plan.getCurrency())
                    .activationSource(ActivationSource.ADMIN_MANUAL)
                    .notes("Comp trial granted by rollout migration to preserve existing live status.")
                    .build();
            subscriptionRepository.save(sub);
            subscriptionGate.recomputeVenueLiveFlag(venue.getId());
            granted++;
            log.info("Comp trial granted to existing live venue {} on plan {}", venue.getId(), plan.getCode());
        }
        if (granted > 0) {
            log.info("Existing-venue subscription migration granted {} comp trial(s).", granted);
        }
    }

    /** Smallest plan whose maxCourts covers the venue's courts; falls back to the largest plan. */
    private SubscriptionPlanEntity smallestPlanFor(List<SubscriptionPlanEntity> plans, long courts) {
        SubscriptionPlanEntity largest = plans.get(0);
        SubscriptionPlanEntity best = null;
        for (SubscriptionPlanEntity p : plans) {
            if (p.getMaxCourts() > largest.getMaxCourts()) largest = p;
            if (p.getMaxCourts() >= courts && (best == null || p.getMaxCourts() < best.getMaxCourts())) {
                best = p;
            }
        }
        return best != null ? best : largest;
    }
}
