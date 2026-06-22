package com.turfbook.backend.entity;

/**
 * Capabilities a plan can grant. Server-side feature gating checks the active
 * subscription's snapshot feature set against these codes.
 */
public enum FeatureCode {
    AUTO_ACCEPT,
    OFFERS,
    ANALYTICS,
    ADVANCED_ANALYTICS,
    PRIORITY_PLACEMENT,
    FEATURED_BADGE,
    PRIORITY_SUPPORT
}
