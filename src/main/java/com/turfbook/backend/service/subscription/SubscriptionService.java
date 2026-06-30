package com.turfbook.backend.service.subscription;

import com.turfbook.backend.dto.ActivateChangeRequest;
import com.turfbook.backend.dto.CourtChangeRequest;
import com.turfbook.backend.dto.CreateCourtChangeRequestBody;
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

    /** Courts of the request's venue, flagged active + requested-covered, for the admin court picker. */
    List<SelectableCourt> adminListChangeRequestCourts(Long requestId);

    /**
     * Approve + activate a pending change request. {@code request} is optional; when it carries a
     * non-empty {@code courtIds}, that admin-chosen coverage replaces the owner's selection
     * (validated ≤ plan limit, all belonging to the venue). Pass null to keep the owner's selection.
     */
    Subscription adminActivateChangeRequest(Long requestId, ActivateChangeRequest request, Long adminId);

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

    /**
     * Owner toggles whether one court is LIVE (player-bookable) by adding/removing it from the
     * subscription's covered set. DRAFT→LIVE into a free slot is allowed (cap-guarded, COURT_LIVE_LIMIT);
     * deactivating an already-LIVE court is LOCKED for owners (COURT_LIVE_LOCKED — use a court-change
     * request). Idempotent. Returns the refreshed state.
     */
    VenueSubscriptionState ownerSetCourtLive(Long venueId, Long courtId, Long ownerId, boolean live);

    // ─── Court-change requests (owner files; super-admin approves) ─────────────

    /** This venue's court-change requests, newest first (owner-scoped). */
    List<CourtChangeRequest> ownerListCourtChangeRequests(Long venueId, Long ownerId);

    /** Owner files a request to free (and optionally swap) an already-LIVE court. Throws 409 if invalid. */
    CourtChangeRequest ownerCreateCourtChangeRequest(Long venueId, Long ownerId, CreateCourtChangeRequestBody body);

    /** Owner cancels their own pending court-change request. */
    CourtChangeRequest ownerCancelCourtChangeRequest(Long venueId, Long requestId, Long ownerId);

    /** Super-admin queue of court-change requests by status (default PENDING). Super-admin only. */
    List<CourtChangeRequest> adminListCourtChangeRequests(String status);

    /** Super-admin approves + applies a court-change request (re-validates; auto-rejects if stale). */
    CourtChangeRequest adminApproveCourtChangeRequest(Long id);

    /** Super-admin rejects a court-change request with a reason. */
    CourtChangeRequest adminRejectCourtChangeRequest(Long id, String reason);
}
