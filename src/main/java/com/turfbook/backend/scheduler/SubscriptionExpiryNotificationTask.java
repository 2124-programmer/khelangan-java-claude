package com.turfbook.backend.scheduler;

import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.service.MailService;
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
 * Emits owner reminders (in-app + email) as a live-gating subscription nears its end (trial or paid
 * period). The first reminder fires within the last 5 days and escalates (5 → 3 → 1 days), staying
 * idempotent via {@link SubscriptionEntity#getExpiryNotifiedThreshold()} so a restart or a second
 * hourly run never re-sends the same reminder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryNotificationTask {

    /** Reminder thresholds in days, most-urgent last. First reminder is in the last 5 days. */
    private static final int[] THRESHOLDS = {5, 3, 1};

    private static final List<SubscriptionStatus> LIVE_GATING =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE);

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;
    private final MailService mailService;
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

        // Best-effort email in addition to the in-app notification (mail send is async + swallows
        // its own failures, so it never breaks the reminder bookkeeping below).
        String email = ownerEmail(sub.getOwner());
        if (email != null) {
            mailService.sendSubscriptionExpiryWarning(email, sub.getVenue().getName(), (int) remainingDays, trial);
        }

        sub.setExpiryNotifiedThreshold(crossed);
        subscriptionRepository.save(sub);
        log.info("Sent {}-day expiry reminder for subscription {} (venue {})",
                crossed, sub.getId(), sub.getVenue().getId());
    }

    /** The owner's deliverable email: the active (login) email, falling back to the original. */
    static String ownerEmail(UserEntity owner) {
        if (owner == null) return null;
        if (owner.getActiveEmail() != null && !owner.getActiveEmail().isBlank()) return owner.getActiveEmail();
        return owner.getEmail() != null && !owner.getEmail().isBlank() ? owner.getEmail() : null;
    }
}
