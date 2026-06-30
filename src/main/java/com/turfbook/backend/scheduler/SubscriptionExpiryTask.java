package com.turfbook.backend.scheduler;

import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.MailService;
import com.turfbook.backend.service.NotificationService;
import com.turfbook.backend.service.subscription.SubscriptionDateCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Drives the subscription lifecycle past its period end:
 * <ol>
 *   <li>TRIALING/ACTIVE whose periodEnd has passed → PAST_DUE (a 5-day grace begins).
 *       The venue is suspended immediately (hidden from players, new bookings blocked).</li>
 *   <li>PAST_DUE whose grace window has elapsed → EXPIRED (terminal; becomes history).</li>
 * </ol>
 * Confirmed future bookings are never touched here, so they are honored; admin renew restores live.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryTask {

    private static final List<SubscriptionStatus> LIVE_GATING =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE);

    /** Statuses a subscription can be in after its period ended (courts already hidden from players). */
    private static final List<SubscriptionStatus> POST_PERIOD =
            List.of(SubscriptionStatus.PAST_DUE, SubscriptionStatus.EXPIRED);

    /** Days after period end to nudge the owner to purchase/renew and relist. */
    private static final int POST_EXPIRY_REMINDER_DAYS = 3;

    private final SubscriptionRepository subscriptionRepository;
    private final VenueRepository venueRepository;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final SubscriptionDateCalculator dates;

    /** Period end → PAST_DUE + suspend venue. Runs hourly. */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void markPastDue() {
        LocalDateTime now = dates.now();
        List<SubscriptionEntity> ended = subscriptionRepository
                .findByStatusInAndPeriodEndBefore(LIVE_GATING, now);
        if (ended.isEmpty()) return;
        log.info("Subscription lifecycle: moving {} subscription(s) to PAST_DUE", ended.size());
        for (SubscriptionEntity sub : ended) {
            try {
                sub.setStatus(SubscriptionStatus.PAST_DUE);
                subscriptionRepository.save(sub);
                suspendVenue(sub.getVenue());
                notify(sub, "Subscription past due",
                        String.format("Your venue '%s' subscription has lapsed and is no longer visible to players. "
                                + "Renew within %d days to go live again.",
                                sub.getVenue().getName(), SubscriptionDateCalculator.GRACE_DAYS));
            } catch (Exception e) {
                log.error("Failed to mark subscription {} PAST_DUE — skipping", sub.getId(), e);
            }
        }
    }

    /** Grace end → EXPIRED (terminal). Runs hourly. */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void expirePastDue() {
        LocalDateTime now = dates.now();
        List<SubscriptionEntity> pastDue = subscriptionRepository.findByStatus(SubscriptionStatus.PAST_DUE);
        if (pastDue.isEmpty()) return;
        for (SubscriptionEntity sub : pastDue) {
            if (now.isAfter(dates.graceCutoff(sub.getPeriodEnd()))) {
                try {
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepository.save(sub);
                    suspendVenue(sub.getVenue());
                    notify(sub, "Subscription expired",
                            String.format("Your venue '%s' subscription has expired. "
                                    + "Contact the admin to renew and go live again.", sub.getVenue().getName()));
                    log.info("Subscription {} (venue {}) EXPIRED after grace", sub.getId(), sub.getVenue().getId());
                } catch (Exception e) {
                    log.error("Failed to expire subscription {} — skipping", sub.getId(), e);
                }
            }
        }
    }

    /**
     * A few days after a venue's period ended (and its courts went dark), nudge the owner — in-app +
     * email — to purchase/renew and relist. One-shot per period via {@code postExpiryReminderSent},
     * which {@code snapshot()} resets on every fresh activation/renew. Runs hourly.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void remindAfterExpiry() {
        LocalDateTime now = dates.now();
        List<SubscriptionEntity> postPeriod = subscriptionRepository.findByStatusIn(POST_PERIOD);
        for (SubscriptionEntity sub : postPeriod) {
            if (sub.isPostExpiryReminderSent() || sub.getPeriodEnd() == null) continue;
            if (now.isBefore(sub.getPeriodEnd().plusDays(POST_EXPIRY_REMINDER_DAYS))) continue;
            try {
                notify(sub, "Purchase a plan to relist your venue",
                        String.format("Your venue '%s' is hidden from players because its subscription ended. "
                                + "Purchase or renew a plan to go live again — your courts and bookings are safe.",
                                sub.getVenue().getName()));
                String email = SubscriptionExpiryNotificationTask.ownerEmail(sub.getOwner());
                if (email != null) {
                    mailService.sendSubscriptionExpiredReminder(email, sub.getVenue().getName());
                }
                sub.setPostExpiryReminderSent(true);
                subscriptionRepository.save(sub);
                log.info("Sent post-expiry purchase reminder for subscription {} (venue {})",
                        sub.getId(), sub.getVenue().getId());
            } catch (Exception e) {
                log.warn("Failed post-expiry reminder for subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    /** Suspend visibility: clear the denormalized live flag + placement weight. Bookings untouched. */
    private void suspendVenue(VenueEntity venue) {
        boolean changed = venue.isSubscriptionActive() || venue.getPlacementWeight() != 0 || venue.isFeatured();
        venue.setSubscriptionActive(false);
        venue.setPlacementWeight(0);
        venue.setFeatured(false);
        if (changed) venueRepository.save(venue);
    }

    private void notify(SubscriptionEntity sub, String title, String body) {
        try {
            notificationService.createNotification(sub.getOwner(), title, body,
                    NotificationEntity.NotificationType.SYSTEM);
        } catch (Exception e) {
            log.warn("Failed to notify owner of subscription {}: {}", sub.getId(), e.getMessage());
        }
    }
}
