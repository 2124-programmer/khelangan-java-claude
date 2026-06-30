package com.turfbook.backend.controller;

import com.turfbook.backend.api.OwnerSubscriptionPurchaseApi;
import com.turfbook.backend.dto.CourtChangeRequest;
import com.turfbook.backend.dto.CourtLiveBody;
import com.turfbook.backend.dto.CourtSelectionBody;
import com.turfbook.backend.dto.CreateCourtChangeRequestBody;
import com.turfbook.backend.dto.PaidRequestBody;
import com.turfbook.backend.dto.PlanOption;
import com.turfbook.backend.dto.SelectableCourt;
import com.turfbook.backend.dto.SubscriptionRequestView;
import com.turfbook.backend.dto.VenueSubscriptionState;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Owner-facing subscription purchase surface: the self-serve trial + paid-plan requests with
 * court-coverage selection. Distinct from {@link OwnerSubscriptionController} (the older upgrade
 * flow) so neither contract clashes. All routes are owner-scoped to the venue's owner in the service.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerSubscriptionPurchaseController implements OwnerSubscriptionPurchaseApi {

    private final SubscriptionService subscriptionService;

    @Override
    public ResponseEntity<List<PlanOption>> getOwnerVenuePlanOptions(Long venueId) {
        return ResponseEntity.ok(subscriptionService.ownerListPlanOptions(venueId, currentUserId()));
    }

    @Override
    public ResponseEntity<VenueSubscriptionState> getOwnerVenueSubscriptionState(Long venueId) {
        return ResponseEntity.ok(subscriptionService.ownerGetVenueSubscriptionState(venueId, currentUserId()));
    }

    @Override
    public ResponseEntity<List<SelectableCourt>> getOwnerVenueSelectableCourts(Long venueId) {
        return ResponseEntity.ok(subscriptionService.ownerListSelectableCourts(venueId, currentUserId()));
    }

    @Override
    public ResponseEntity<VenueSubscriptionState> startVenueTrial(Long venueId, CourtSelectionBody body) {
        return ResponseEntity.ok(subscriptionService.ownerStartTrial(venueId, currentUserId(), body));
    }

    @Override
    public ResponseEntity<SubscriptionRequestView> createVenueSubscriptionRequest(Long venueId, PaidRequestBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.ownerCreateSubscriptionRequest(venueId, currentUserId(), body));
    }

    @Override
    public ResponseEntity<VenueSubscriptionState> cancelVenueSubscriptionRequest(Long venueId) {
        return ResponseEntity.ok(subscriptionService.ownerCancelSubscriptionRequest(venueId, currentUserId()));
    }

    @Override
    public ResponseEntity<VenueSubscriptionState> setVenueCourtLive(Long venueId, Long courtId, CourtLiveBody body) {
        return ResponseEntity.ok(subscriptionService.ownerSetCourtLive(
                venueId, courtId, currentUserId(), Boolean.TRUE.equals(body.getLive())));
    }

    @Override
    public ResponseEntity<java.util.List<CourtChangeRequest>> listOwnerCourtChangeRequests(Long venueId) {
        return ResponseEntity.ok(subscriptionService.ownerListCourtChangeRequests(venueId, currentUserId()));
    }

    @Override
    public ResponseEntity<CourtChangeRequest> createCourtChangeRequest(Long venueId, CreateCourtChangeRequestBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.ownerCreateCourtChangeRequest(venueId, currentUserId(), body));
    }

    @Override
    public ResponseEntity<CourtChangeRequest> cancelCourtChangeRequest(Long venueId, Long requestId) {
        return ResponseEntity.ok(subscriptionService.ownerCancelCourtChangeRequest(venueId, requestId, currentUserId()));
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
