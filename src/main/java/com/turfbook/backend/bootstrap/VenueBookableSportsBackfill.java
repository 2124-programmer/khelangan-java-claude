package com.turfbook.backend.bootstrap;

import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Backfill the denormalized {@code venue.bookableSportIds} for existing data.
 *
 * <p>This set (sports that have at least one player-bookable court) is recomputed inside
 * {@code recomputeVenueLive}, alongside {@code bookableCourtCount}, on every subscription/court
 * transition — and discovery's sport filter now matches against it instead of the static
 * {@code venue_sports} list. Venues last touched before the field existed carry an empty set, which
 * would drop them from the player feed's sport filter until their next transition. Recomputing every
 * venue that holds a current (TRIALING/ACTIVE/PAST_DUE) subscription on startup closes that gap.
 *
 * <p>Idempotent: {@code recomputeVenueLive} is deterministic, so re-running on each boot converges
 * to the same denormalized values. Ordered after {@link ExistingVenueSubscriptionMigration} (@Order 2)
 * so any coverage it backfills is reflected here.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class VenueBookableSportsBackfill implements ApplicationRunner {

    private static final List<SubscriptionStatus> CURRENT =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionGate subscriptionGate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<Long> recomputed = new HashSet<>();
        for (SubscriptionStatus status : CURRENT) {
            for (SubscriptionEntity sub : subscriptionRepository.findByStatus(status)) {
                Long venueId = sub.getVenue() != null ? sub.getVenue().getId() : null;
                if (venueId == null || !recomputed.add(venueId)) continue;
                subscriptionGate.recomputeVenueLiveFlag(venueId);
            }
        }
        if (!recomputed.isEmpty()) {
            log.info("Recomputed denormalized bookable sports/courts for {} venue(s) on startup.", recomputed.size());
        }
    }
}
