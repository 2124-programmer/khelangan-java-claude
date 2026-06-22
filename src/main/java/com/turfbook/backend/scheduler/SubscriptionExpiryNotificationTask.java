package com.turfbook.backend.scheduler;

import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.service.NotificationService;
import com.turfbook.backend.service.subscription.SubscriptionDateCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Emits owner reminders as a live-gating subscription nears its end (trial or paid period).
 * Notifies once per threshold (7 → 3 → 1 days) and escalates as the deadline nears, staying
 * idempotent via {@link SubscriptionEntity#getExpiryNotifiedThreshold()} so a restart or a second
 * hourly run never re-sends the same reminder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryNotificationTask {

    /** Reminder thresholds in days, most-urgent last. */
    private static final int[] THRESHOLDS = {7, 3, 1};

    private static final List<SubscriptionStatus> LIVE_GATING =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE);

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;
    private final SubscriptionDateCalculator dates;

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void sendExpiryReminders() {
        LocalDateTime now = dates.now();
        List<SubscriptionEntity> live = subscriptionRepository.findByStatusIn(LIVE_GATING);
        for (SubscriptionEntity sub : live) {
            try {
                maybeNotify(sub, now);
            } catch (Exception e) {
                log.warn("Failed expiry reminder for subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    private void maybeNotify(SubscriptionEntity sub, LocalDateTime now) {
        LocalDateTime end = sub.getStatus() == SubscriptionStatus.TRIALING && sub.getTrialEnd() != null
                ? sub.getTrialEnd() : sub.getPeriodEnd();
        if (end == null) return;

        long remainingDays = ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate());
        if (remainingDays < 1) return; // already past/at end → handled by the suspend/expire job

        // Most-urgent threshold the remaining days have crossed (smallest T with remaining <= T).
        Integer crossed = null;
        for (int t : THRESHOLDS) {
            if (remainingDays <= t) crossed = t;
        }
        if (crossed == null) return; // more than the largest threshold away

        Integer alreadyNotified = sub.getExpiryNotifiedThreshold();
        if (alreadyNotified != null && crossed >= alreadyNotified) return; // same/less-urgent already sent

        boolean trial = sub.getStatus() == SubscriptionStatus.TRIALING;
        String title = trial ? "Trial ending soon" : "Subscription ending soon";
        String body = String.format(
                "Your venue '%s' %s ends in %d day%s. %s to stay live.",
                sub.getVenue().getName(),
                trial ? "free trial" : "subscription",
                remainingDays, remainingDays == 1 ? "" : "s",
                trial ? "Activate a paid plan" : "Renew");
        notificationService.createNotification(sub.getOwner(), title, body,
                NotificationEntity.NotificationType.SYSTEM);

        sub.setExpiryNotifiedThreshold(crossed);
        subscriptionRepository.save(sub);
        log.info("Sent {}-day expiry reminder for subscription {} (venue {})",
                crossed, sub.getId(), sub.getVenue().getId());
    }
}
