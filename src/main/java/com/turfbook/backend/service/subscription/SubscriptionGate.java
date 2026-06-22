package com.turfbook.backend.service.subscription;

import com.turfbook.backend.entity.FeatureCode;
import com.turfbook.backend.entity.SubscriptionEntity;

import java.util.Optional;

/**
 * Read-only enforcement API consumed by other services (booking, court creation,
 * photo upload, analytics, venue listing). Kept separate from {@code SubscriptionService}
 * so collaborators depend only on the gating surface, not the full admin/owner workflow.
 *
 * <p>The server is the source of truth: gated capabilities reject with 403 when the
 * venue's current plan lacks the feature; the client mirrors these gates for UX only.
 */
public interface SubscriptionGate {

    /** The current (non-terminal) subscription for a venue, if any. */
    Optional<SubscriptionEntity> currentSubscription(Long venueId);

    /** True when the venue is approved (status LIVE) AND holds a live-gating subscription. */
    boolean isVenueLive(Long venueId);

    /** True when the venue's current plan grants {@code feature}. */
    boolean hasFeature(Long venueId, FeatureCode feature);

    /** True when any of the owner's venues holds a live plan granting {@code feature}. */
    boolean ownerHasFeatureOnAnyVenue(Long ownerId, FeatureCode feature);

    /** Throws {@link com.turfbook.backend.exception.ForbiddenException} (403) if absent. */
    void assertFeature(Long venueId, FeatureCode feature, String message);

    /** Court limit from the current subscription's snapshot, or 0 when there is none. */
    int maxCourtsFor(Long venueId);

    /** Photo limit from the current subscription's plan, or 0 when there is none. */
    int photoLimitFor(Long venueId);

    /**
     * Asserts a new court may be added: requires a live-gating subscription and that
     * the current court count is below the plan's {@code maxCourts}. Throws 409 otherwise.
     */
    void assertCanAddCourt(Long venueId);

    /** Recompute and persist the venue's denormalized live flag, placement weight, and featured badge. */
    void recomputeVenueLiveFlag(Long venueId);
}
