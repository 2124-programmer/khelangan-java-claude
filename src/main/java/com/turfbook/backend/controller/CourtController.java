package com.turfbook.backend.controller;

import com.turfbook.backend.api.CourtsApi;
import com.turfbook.backend.dto.CourtDto;
import com.turfbook.backend.dto.CreateCourtRequest;
import com.turfbook.backend.dto.UpdateCourtRequest;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.VenueService;
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
public class CourtController implements CourtsApi {

    private final VenueService venueService;

    @Override
    public ResponseEntity<List<CourtDto>> listCourts(Long venueId) {
        log.info("CourtController.listCourts() called - venueId={}", venueId);
        return ResponseEntity.ok(venueService.listCourts(venueId));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CourtDto> createCourt(Long venueId, CreateCourtRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("CourtController.createCourt() called - venueId={}, ownerId={}", venueId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.createCourt(venueId, principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CourtDto> updateCourt(Long venueId, Long courtId, UpdateCourtRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("CourtController.updateCourt() called - venueId={}, courtId={}, ownerId={}", venueId, courtId, principal.getId());
        return ResponseEntity.ok(venueService.updateCourt(venueId, courtId, principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteCourt(Long venueId, Long courtId) {
        UserPrincipal principal = getPrincipal();
        log.info("CourtController.deleteCourt() called - venueId={}, courtId={}, ownerId={}", venueId, courtId, principal.getId());
        venueService.deleteCourt(venueId, courtId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
