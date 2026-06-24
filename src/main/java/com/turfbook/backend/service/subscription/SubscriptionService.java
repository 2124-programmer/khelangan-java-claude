package com.turfbook.backend.service.subscription;

import com.turfbook.backend.dto.CourtSelectionBody;
import com.turfbook.backend.dto.PaidRequestBody;
import com.turfbook.backend.dto.PlanOption;
import com.turfbook.backend.dto.RejectChangeRequest;
import com.turfbook.backend.dto.SelectableCourt;
import com.turfbook.backend.dto.Subscription;
import com.turfbook.backend.dto.SubscriptionChangeRequest;
import com.turfbook.backend.dto.SubscriptionCreateRequest;
import com.turfbook.backend.dto.SubscriptionEditRequest;
import com.turfbook.backend.dto.SubscriptionPage;
import com.turfbook.backend.dto.SubscriptionPlan;
import com.turfbook.backend.dto.SubscriptionRequestView;
import com.turfbook.backend.dto.UpdatePlanRequest;
import com.turfbook.backend.dto.UpgradeRequestCreate;
import com.turfbook.backend.dto.VenueSubscriptionPage;
import com.turfbook.backend.dto.VenueSubscriptionState;
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

    /** Searchable, paginated table of every venue's current subscription (admin). */
    VenueSubscriptionPage adminListVenueSubscriptions(String q, String status, int page, int size);

    VenueSubscriptionView adminGetVenueSubscription(Long venueId);

    // ─── Admin: change requests ─────────────────────────────────────────────
    List<SubscriptionChangeRequest> adminListChangeRequests(String status);

    Subscription adminActivateChangeRequest(Long requestId, Long adminId);

    SubscriptionChangeRequest adminRejectChangeRequest(Long requestId, RejectChangeRequest request);

    // ─── Owner ──────────────────────────────────────────────────────────────
    List<SubscriptionPlan> ownerListActivePlans();

    VenueSubscriptionView ownerGetVenueSubscription(Long venueId, Long ownerId);

    SubscriptionChangeRequest ownerCreateUpgradeRequest(Long venueId, Long ownerId, UpgradeRequestCreate request);

    // ─── Owner: court-coverage purchase (self-serve trial + paid request) ─────
    /** Purchasable options for a venue: the self-serve Trial plus paid tiers, with eligibility. */
    List<PlanOption> ownerListPlanOptions(Long venueId, Long ownerId);

    /** Court-coverage subscription state + purchase eligibility for the owner Subscription card. */
    VenueSubscriptionState ownerGetVenueSubscriptionState(Long venueId, Long ownerId);

    /** Courts for the coverage-selection sheet, each flagged active + currently-covered. */
    List<SelectableCourt> ownerListSelectableCourts(Long venueId, Long ownerId);

    /** Self-serve: start the 30-day trial covering the selected courts (≤2). Throws 409 if ineligible. */
    VenueSubscriptionState ownerStartTrial(Long venueId, Long ownerId, CourtSelectionBody body);

    /** Request a paid plan covering the selected courts (≤ plan limit); admin activates it offline. */
    SubscriptionRequestView ownerCreateSubscriptionRequest(Long venueId, Long ownerId, PaidRequestBody body);

    /** Cancel the venue's pending paid-plan request; returns the refreshed state. Throws 409 if none. */
    VenueSubscriptionState ownerCancelSubscriptionRequest(Long venueId, Long ownerId);
}
