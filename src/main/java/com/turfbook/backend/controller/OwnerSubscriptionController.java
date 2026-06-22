package com.turfbook.backend.controller;

import com.turfbook.backend.api.OwnerSubscriptionsApi;
import com.turfbook.backend.dto.SubscriptionChangeRequest;
import com.turfbook.backend.dto.SubscriptionPlan;
import com.turfbook.backend.dto.UpgradeRequestCreate;
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
@PreAuthorize("hasRole('OWNER')")
public class OwnerSubscriptionController implements OwnerSubscriptionsApi {

    private final SubscriptionService subscriptionService;

    @Override
    public ResponseEntity<List<SubscriptionPlan>> listOwnerSubscriptionPlans() {
        return ResponseEntity.ok(subscriptionService.ownerListActivePlans());
    }

    @Override
    public ResponseEntity<VenueSubscriptionView> getOwnerVenueSubscription(Long venueId) {
        return ResponseEntity.ok(subscriptionService.ownerGetVenueSubscription(venueId, currentUserId()));
    }

    @Override
    public ResponseEntity<SubscriptionChangeRequest> createUpgradeRequest(Long venueId, UpgradeRequestCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.ownerCreateUpgradeRequest(venueId, currentUserId(), request));
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
