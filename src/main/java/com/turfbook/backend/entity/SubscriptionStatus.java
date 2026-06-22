package com.turfbook.backend.entity;

/**
 * Lifecycle states for a venue subscription. The set is payment-source-agnostic:
 * a future provider webhook can drive the same transitions that admin actions do.
 *
 * <ul>
 *   <li>{@link #TRIALING} / {@link #ACTIVE} — the "current", live-gating states.</li>
 *   <li>{@link #PAST_DUE} — period ended; inside the grace window (not yet live-gating).</li>
 *   <li>{@link #EXPIRED} / {@link #CANCELED} / {@link #VOIDED} — terminal; form history.</li>
 * </ul>
 */
public enum SubscriptionStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    EXPIRED,
    CANCELED,
    VOIDED;

    /** A venue may be live to players only while its current subscription is in one of these. */
    public boolean isLiveGating() {
        return this == TRIALING || this == ACTIVE;
    }

    /** Terminal states never gate visibility and are retained purely as history. */
    public boolean isTerminal() {
        return this == EXPIRED || this == CANCELED || this == VOIDED;
    }
}
