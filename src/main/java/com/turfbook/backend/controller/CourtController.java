package com.turfbook.backend.controller;

import com.turfbook.backend.api.CourtsApi;
import com.turfbook.backend.dto.CourtDto;
import com.turfbook.backend.dto.CreateCourtRequest;
import com.turfbook.backend.dto.UpdateCourtRequest;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CourtController implements CourtsApi {

    private final VenueService venueService;

    @Override
    public ResponseEntity<List<CourtDto>> listCourts(Long venueId) {
        return ResponseEntity.ok(venueService.listCourts(venueId));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CourtDto> createCourt(Long venueId, CreateCourtRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.createCourt(venueId, principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CourtDto> updateCourt(Long venueId, Long courtId, UpdateCourtRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(venueService.updateCourt(venueId, courtId, principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteCourt(Long venueId, Long courtId) {
        UserPrincipal principal = getPrincipal();
        venueService.deleteCourt(venueId, courtId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
