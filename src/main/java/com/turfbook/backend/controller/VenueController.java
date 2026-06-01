package com.turfbook.backend.controller;

import com.turfbook.backend.api.VenuesApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VenueController implements VenuesApi {

    private final VenueService venueService;

    @Override
    public ResponseEntity<VenueSummaryPage> listVenues(String city, String sport, String search,
                                                        Integer page, Integer size) {
        return ResponseEntity.ok(venueService.listVenues(city, sport, search,
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<VenueDetailDto> getVenue(Long id) {
        return ResponseEntity.ok(venueService.getVenue(id));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<VenueDetailDto> createVenue(CreateVenueRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.createVenue(principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<VenueDetailDto> updateVenue(Long id, UpdateVenueRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(venueService.updateVenue(id, principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VenueDetailDto> updateVenueStatus(Long id, VenueStatusRequest request) {
        return ResponseEntity.ok(venueService.updateVenueStatus(id, request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<VenueSummaryPage> listOwnerVenues(Integer page, Integer size) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(venueService.listOwnerVenues(principal.getId(),
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerStatsDto> getOwnerStats() {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(venueService.getOwnerStats(principal.getId()));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
