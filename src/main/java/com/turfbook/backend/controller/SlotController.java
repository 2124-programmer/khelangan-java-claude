package com.turfbook.backend.controller;

import com.turfbook.backend.api.SlotsApi;
import com.turfbook.backend.dto.BulkBlockRequest;
import com.turfbook.backend.dto.SlotDto;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SlotController implements SlotsApi {

    private final VenueService venueService;

    @Override
    public ResponseEntity<List<SlotDto>> listSlots(Long courtId, LocalDate date) {
        return ResponseEntity.ok(venueService.listSlots(courtId, date));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SlotDto> blockSlot(Long id) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(venueService.blockSlot(id, principal.getId()));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SlotDto> unblockSlot(Long id) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(venueService.unblockSlot(id, principal.getId()));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<SlotDto>> bulkBlockSlots(Long courtId, BulkBlockRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(venueService.bulkBlockSlots(courtId, principal.getId(), request));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
