package com.turfbook.backend.service.subscription;

import com.turfbook.backend.dto.RejectChangeRequest;
import com.turfbook.backend.dto.Subscription;
import com.turfbook.backend.dto.SubscriptionChangeRequest;
import com.turfbook.backend.dto.SubscriptionCreateRequest;
import com.turfbook.backend.dto.SubscriptionEditRequest;
import com.turfbook.backend.dto.SubscriptionPage;
import com.turfbook.backend.dto.SubscriptionPlan;
import com.turfbook.backend.dto.UpdatePlanRequest;
import com.turfbook.backend.dto.UpgradeRequestCreate;
import com.turfbook.backend.dto.VenueSubscriptionView;

import java.util.List;

/**
 * Controller-facing subscription workflow (admin manual management + owner self-service).
 * All returned period dates are server-computed; no create/edit input carries dates.
 */
public interface SubscriptionService {

    // ─── Admin: plan catalog ────────────────────────────────────────────────
    List<SubscriptionPlan> adminListPlans();

    SubscriptionPlan adminUpdatePlan(Long planId, UpdatePlanRequest request);

    // ─── Admin: subscriptions ───────────────────────────────────────────────
    Subscription adminCreateSubscription(SubscriptionCreateRequest request, Long adminId);

    Subscription adminEditSubscription(Long subscriptionId, SubscriptionEditRequest request);

    Subscription adminVoidSubscription(Long subscriptionId);

    Subscription adminRenewSubscription(Long subscriptionId, Long adminId);

    SubscriptionPage adminListSubscriptions(Long venueId, Long ownerId, String status, int page, int size);

    VenueSubscriptionView adminGetVenueSubscription(Long venueId);

    // ─── Admin: change requests ─────────────────────────────────────────────
    List<SubscriptionChangeRequest> adminListChangeRequests(String status);

    Subscription adminActivateChangeRequest(Long requestId, Long adminId);

    SubscriptionChangeRequest adminRejectChangeRequest(Long requestId, RejectChangeRequest request);

    // ─── Owner ──────────────────────────────────────────────────────────────
    List<SubscriptionPlan> ownerListActivePlans();

    VenueSubscriptionView ownerGetVenueSubscription(Long venueId, Long ownerId);

    SubscriptionChangeRequest ownerCreateUpgradeRequest(Long venueId, Long ownerId, UpgradeRequestCreate request);
}
