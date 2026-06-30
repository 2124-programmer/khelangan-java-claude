package com.turfbook.backend.controller;

import com.turfbook.backend.api.AdminSubscriptionsApi;
import com.turfbook.backend.dto.ActivateChangeRequest;
import com.turfbook.backend.dto.CourtChangeRequest;
import com.turfbook.backend.dto.RejectChangeRequest;
import com.turfbook.backend.dto.RejectCourtChangeRequestBody;
import com.turfbook.backend.dto.SelectableCourt;
import com.turfbook.backend.dto.Subscription;
import com.turfbook.backend.dto.SubscriptionChangeRequest;
import com.turfbook.backend.dto.SubscriptionCreateRequest;
import com.turfbook.backend.dto.SubscriptionEditRequest;
import com.turfbook.backend.dto.SubscriptionPage;
import com.turfbook.backend.dto.SubscriptionPlan;
import com.turfbook.backend.dto.UpdatePlanRequest;
import com.turfbook.backend.dto.VenueSubscriptionPage;
import com.turfbook.backend.dto.VenueSubscriptionView;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubscriptionController implements AdminSubscriptionsApi {

    private final SubscriptionService subscriptionService;

    @Override
    public ResponseEntity<List<SubscriptionPlan>> adminListSubscriptionPlans() {
        return ResponseEntity.ok(subscriptionService.adminListPlans());
    }

    @Override
    public ResponseEntity<SubscriptionPlan> adminUpdateSubscriptionPlan(Long id, UpdatePlanRequest request) {
        return ResponseEntity.ok(subscriptionService.adminUpdatePlan(id, request));
    }

    @Override
    public ResponseEntity<Subscription> adminCreateSubscription(SubscriptionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.adminCreateSubscription(request, currentUserId()));
    }

    @Override
    public ResponseEntity<Subscription> adminEditSubscription(Long id, SubscriptionEditRequest request) {
        return ResponseEntity.ok(subscriptionService.adminEditSubscription(id, request));
    }

    @Override
    public ResponseEntity<Subscription> adminVoidSubscription(Long id) {
        return ResponseEntity.ok(subscriptionService.adminVoidSubscription(id));
    }

    @Override
    public ResponseEntity<Subscription> adminRenewSubscription(Long id) {
        return ResponseEntity.ok(subscriptionService.adminRenewSubscription(id, currentUserId()));
    }

    @Override
    public ResponseEntity<SubscriptionPage> adminListSubscriptions(Long venueId, Long ownerId, String status,
                                                                   Integer page, Integer size) {
        return ResponseEntity.ok(subscriptionService.adminListSubscriptions(
                venueId, ownerId, status, page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<VenueSubscriptionPage> adminListVenueSubscriptions(String q, String status,
                                                                             Integer page, Integer size) {
        return ResponseEntity.ok(subscriptionService.adminListVenueSubscriptions(
                q, status, page != null ? page : 0, size != null ? size : 15));
    }

    @Override
    public ResponseEntity<VenueSubscriptionView> adminGetVenueSubscription(Long venueId) {
        return ResponseEntity.ok(subscriptionService.adminGetVenueSubscription(venueId));
    }

    @Override
    public ResponseEntity<List<SubscriptionChangeRequest>> adminListChangeRequests(String status) {
        return ResponseEntity.ok(subscriptionService.adminListChangeRequests(status));
    }

    @Override
    public ResponseEntity<List<SelectableCourt>> adminListChangeRequestCourts(Long id) {
        return ResponseEntity.ok(subscriptionService.adminListChangeRequestCourts(id));
    }

    @Override
    public ResponseEntity<Subscription> adminActivateChangeRequest(Long id, ActivateChangeRequest activateChangeRequest) {
        return ResponseEntity.ok(
                subscriptionService.adminActivateChangeRequest(id, activateChangeRequest, currentUserId()));
    }

    @Override
    public ResponseEntity<SubscriptionChangeRequest> adminRejectChangeRequest(Long id, RejectChangeRequest request) {
        return ResponseEntity.ok(subscriptionService.adminRejectChangeRequest(id, request));
    }

    // ─── Court-change requests (super-admin gated in the service) ──────────────

    @Override
    public ResponseEntity<List<CourtChangeRequest>> adminListCourtChangeRequests(String status) {
        return ResponseEntity.ok(subscriptionService.adminListCourtChangeRequests(status));
    }

    @Override
    public ResponseEntity<CourtChangeRequest> adminApproveCourtChangeRequest(Long id) {
        return ResponseEntity.ok(subscriptionService.adminApproveCourtChangeRequest(id));
    }

    @Override
    public ResponseEntity<CourtChangeRequest> adminRejectCourtChangeRequest(Long id, RejectCourtChangeRequestBody body) {
        return ResponseEntity.ok(subscriptionService.adminRejectCourtChangeRequest(id, body.getReason()));
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
