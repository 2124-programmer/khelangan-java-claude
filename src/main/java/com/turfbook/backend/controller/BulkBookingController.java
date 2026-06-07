package com.turfbook.backend.controller;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.dto.BulkCreateBookingRequest;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BulkBookingController {

    private final BookingService bookingService;

    @PostMapping("/api/v1/bookings/bulk")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<List<BookingDto>> bulkCreateBookings(@RequestBody BulkCreateBookingRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("BulkBookingController.bulkCreateBookings() - userId={}, courtId={}, date={}, count={}",
                principal.getId(), request.getCourtId(), request.getDate(),
                request.getStartTimes() == null ? 0 : request.getStartTimes().size());
        List<BookingDto> dtos = bookingService.bulkCreateBookings(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
    }

    @PostMapping("/api/v1/bookings/group/{groupId}/accept")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BookingDto>> acceptBookingGroup(@PathVariable String groupId) {
        UserPrincipal principal = getPrincipal();
        log.info("BulkBookingController.acceptBookingGroup() - groupId={}, ownerId={}", groupId, principal.getId());
        return ResponseEntity.ok(bookingService.acceptBookingGroup(groupId, principal.getId()));
    }

    @PostMapping("/api/v1/bookings/group/{groupId}/reject")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BookingDto>> rejectBookingGroup(@PathVariable String groupId) {
        UserPrincipal principal = getPrincipal();
        log.info("BulkBookingController.rejectBookingGroup() - groupId={}, ownerId={}", groupId, principal.getId());
        return ResponseEntity.ok(bookingService.rejectBookingGroup(groupId, principal.getId()));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
