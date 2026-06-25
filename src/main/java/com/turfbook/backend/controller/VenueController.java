package com.turfbook.backend.controller;

import com.turfbook.backend.api.VenuesApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VenueController implements VenuesApi {

    private final VenueService venueService;

    @Override
    public ResponseEntity<VenueSummaryPage> listVenues(String city, String sport, String search,
                                                        String sort, Double lat, Double lng,
                                                        Integer minPrice, Integer maxPrice,
                                                        Double minRating, Integer page, Integer size) {
        log.info("VenueController.listVenues() called - city={}, sport={}, search={}, sort={}", city, sport, search, sort);
        return ResponseEntity.ok(venueService.listVenues(city, sport, search, sort, lat, lng,
                minPrice, maxPrice, minRating, page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<VenueDetailDto> getVenue(Long id) {
        log.info("VenueController.getVenue() called - id={}", id);
        return ResponseEntity.ok(venueService.getVenue(id));
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<Void> favoriteVenue(Long venueId) {
        UserPrincipal principal = getPrincipal();
        log.info("VenueController.favoriteVenue() called - venueId={}, playerId={}", venueId, principal.getId());
        venueService.favoriteVenue(principal.getId(), venueId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<Void> unfavoriteVenue(Long venueId) {
        UserPrincipal principal = getPrincipal();
        log.info("VenueController.unfavoriteVenue() called - venueId={}, playerId={}", venueId, principal.getId());
        venueService.unfavoriteVenue(principal.getId(), venueId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ContactVenueResponse> contactVenue(Long venueId, ContactVenueRequest contactVenueRequest) {
        UserPrincipal principal = getPrincipal();
        log.info("VenueController.contactVenue() called - venueId={}, playerId={}", venueId, principal.getId());
        ContactVenueResponse body = venueService.contactVenue(principal.getId(), venueId, contactVenueRequest);
        return ResponseEntity.accepted().body(body);
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<VenueDetailDto> createVenue(CreateVenueRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("VenueController.createVenue() called - ownerId={}, name={}", principal.getId(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.createVenue(principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<VenueDetailDto> updateVenue(Long id, UpdateVenueRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("VenueController.updateVenue() called - id={}, ownerId={}", id, principal.getId());
        return ResponseEntity.ok(venueService.updateVenue(id, principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VenueDetailDto> updateVenueStatus(Long id, VenueStatusRequest request) {
        log.info("VenueController.updateVenueStatus() called - id={}, status={}", id, request.getStatus());
        return ResponseEntity.ok(venueService.updateVenueStatus(id, request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<VenueDetailDto> submitVenue(Long venueId, SubmitVenueRequest request) {
        UserPrincipal principal = getPrincipal();
        Long planId = request != null ? request.getPlanId() : null;
        log.info("VenueController.submitVenue() called - venueId={}, ownerId={}, planId={}",
                venueId, principal.getId(), planId);
        return ResponseEntity.ok(venueService.submitVenueForApproval(venueId, principal.getId(), planId));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<VenueSummaryPage> listOwnerVenues(Integer page, Integer size) {
        UserPrincipal principal = getPrincipal();
        log.info("VenueController.listOwnerVenues() called - ownerId={}", principal.getId());
        return ResponseEntity.ok(venueService.listOwnerVenues(principal.getId(),
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerStatsDto> getOwnerStats() {
        UserPrincipal principal = getPrincipal();
        log.info("VenueController.getOwnerStats() called - ownerId={}", principal.getId());
        return ResponseEntity.ok(venueService.getOwnerStats(principal.getId()));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
