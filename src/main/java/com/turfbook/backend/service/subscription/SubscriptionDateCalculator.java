package com.turfbook.backend.service.subscription;

import com.turfbook.backend.entity.BillingCycle;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The single source of truth for subscription period dates. All computation happens
 * in Asia/Kolkata. Clients never supply period dates — the server derives them here.
 */
@Component
public class SubscriptionDateCalculator {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /** Grace window (days) after periodEnd: PAST_DUE before EXPIRED. */
    public static final int GRACE_DAYS = 5;

    /** Current wall-clock time in IST. */
    public LocalDateTime now() {
        return LocalDateTime.now(IST);
    }

    /** Paid period end: +1 month (MONTHLY) or +1 year (ANNUAL) from {@code start}. */
    public LocalDateTime periodEnd(LocalDateTime start, BillingCycle cycle) {
        return cycle == BillingCycle.ANNUAL ? start.plusYears(1) : start.plusMonths(1);
    }

    /** Trial end: {@code start + trialDays}. */
    public LocalDateTime trialEnd(LocalDateTime start, int trialDays) {
        return start.plusDays(Math.max(0, trialDays));
    }

    /** End of the grace window for a subscription whose period ended at {@code periodEnd}. */
    public LocalDateTime graceCutoff(LocalDateTime periodEnd) {
        return periodEnd.plusDays(GRACE_DAYS);
    }
}
