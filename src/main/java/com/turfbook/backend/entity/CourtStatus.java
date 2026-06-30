package com.turfbook.backend.entity;

/**
 * Lifecycle of a court row. Courts are never hard-deleted: removing one is a soft delete so the
 * row (and its slots/booking history) survives for audit and admin inspection.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — a normal court (may be LIVE or LOCKED depending on subscription coverage;
 *       that distinction is derived from coverage, not stored here).</li>
 *   <li>{@link #DELETED} — soft-deleted by the owner. Hidden from the owner and players (looks gone
 *       to them) and never selectable for subscription coverage; visible only to admin/super-admin.</li>
 * </ul>
 */
public enum CourtStatus {
    ACTIVE,
    DELETED
}
