package com.turfbook.backend.controller;

import com.turfbook.backend.api.SlotsApi;
import com.turfbook.backend.dto.BlockSelectedRequest;
import com.turfbook.backend.dto.BulkBlockRequest;
import com.turfbook.backend.dto.CourtSlotsDto;
import com.turfbook.backend.dto.SlotDto;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SlotController implements SlotsApi {

    private final VenueService venueService;

    @Override
    public ResponseEntity<List<SlotDto>> listSlots(Long courtId, LocalDate date) {
        log.info("SlotController.listSlots() called - courtId={}, date={}", courtId, date);
        return ResponseEntity.ok(venueService.listSlots(courtId, date));
    }

    @Override
    public ResponseEntity<List<CourtSlotsDto>> listVenueSlots(Long venueId, LocalDate date, Long sportId) {
        log.info("SlotController.listVenueSlots() called - venueId={}, date={}, sportId={}", venueId, date, sportId);
        return ResponseEntity.ok(venueService.listSlotsByVenue(venueId, date, sportId));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SlotDto> blockSlot(Long id) {
        UserPrincipal principal = getPrincipal();
        log.info("SlotController.blockSlot() called - id={}, ownerId={}", id, principal.getId());
        return ResponseEntity.ok(venueService.blockSlot(id, principal.getId()));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SlotDto> unblockSlot(Long id) {
        UserPrincipal principal = getPrincipal();
        log.info("SlotController.unblockSlot() called - id={}, ownerId={}", id, principal.getId());
        return ResponseEntity.ok(venueService.unblockSlot(id, principal.getId()));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<SlotDto>> bulkBlockSlots(Long courtId, BulkBlockRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("SlotController.bulkBlockSlots() called - courtId={}, ownerId={}, date={}", courtId, principal.getId(), request.getDate());
        return ResponseEntity.ok(venueService.bulkBlockSlots(courtId, principal.getId(), request));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/api/v1/courts/{courtId}/slots/block-by-time")
    public ResponseEntity<SlotDto> blockSlotByTime(
            @PathVariable Long courtId,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        UserPrincipal principal = getPrincipal();
        log.info("SlotController.blockSlotByTime() called - courtId={}, date={}, startTime={}, ownerId={}",
                courtId, date, startTime, principal.getId());
        return ResponseEntity.ok(venueService.blockSlotByTime(courtId, principal.getId(), date, startTime, endTime));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/api/v1/courts/{courtId}/slots/block-selected")
    public ResponseEntity<List<SlotDto>> blockSlotsByTime(
            @PathVariable Long courtId,
            @RequestBody BlockSelectedRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("SlotController.blockSlotsByTime() called - courtId={}, date={}, count={}, ownerId={}",
                courtId, request.getDate(),
                request.getStartTimes() == null ? 0 : request.getStartTimes().size(),
                principal.getId());
        return ResponseEntity.ok(venueService.blockSlotsByTime(courtId, principal.getId(), request));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
