package com.turfbook.backend.bootstrap;

import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Court-coverage backfill (rollout). Court-level booking visibility makes a court bookable only
 * when its id is in the subscription's {@code coveredCourtIds}. Subscriptions created before this
 * feature carry an empty list, which would silently make every existing live venue unbookable.
 *
 * <p>This one-time, idempotent migration grants each current (TRIALING/ACTIVE/PAST_DUE) subscription
 * whose coverage is empty the venue's active courts (capped at the plan's max), then recomputes the
 * venue's denormalized bookable-court count. It NEVER creates a subscription — approval is now a
 * self-serve step (the owner starts a trial / requests a paid plan and picks courts), so a LIVE
 * venue with no subscription is an intentional "not yet activated" state and is left untouched.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ExistingVenueSubscriptionMigration implements ApplicationRunner {

    private static final List<SubscriptionStatus> CURRENT =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final SubscriptionRepository subscriptionRepository;
    private final CourtRepository courtRepository;
    private final SubscriptionGate subscriptionGate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int backfilled = 0;
        for (SubscriptionStatus status : CURRENT) {
            for (SubscriptionEntity sub : subscriptionRepository.findByStatus(status)) {
                List<String> covered = sub.getCoveredCourtIds();
                if (covered != null && !covered.isEmpty()) {
                    continue; // already has a court selection — leave it as-is
                }
                VenueEntity venue = sub.getVenue();
                List<String> ids = courtRepository.findByVenue(venue).stream()
                        .filter(CourtEntity::isActive)
                        .sorted(Comparator.comparing(CourtEntity::getId))
                        .limit(Math.max(0, sub.getMaxCourts()))
                        .map(c -> String.valueOf(c.getId()))
                        .toList();
                if (ids.isEmpty()) {
                    continue; // no active courts to cover
                }
                sub.setCoveredCourtIds(new ArrayList<>(ids));
                subscriptionRepository.save(sub);
                subscriptionGate.recomputeVenueLiveFlag(venue.getId());
                backfilled++;
                log.info("Backfilled coverage for subscription {} (venue {}): {} court(s)",
                        sub.getId(), venue.getId(), ids.size());
            }
        }
        if (backfilled > 0) {
            log.info("Subscription court-coverage backfill updated {} subscription(s).", backfilled);
        }
    }
}
