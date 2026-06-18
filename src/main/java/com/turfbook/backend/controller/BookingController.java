package com.turfbook.backend.controller;

import com.turfbook.backend.api.BookingsApi;
import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.dto.BookingPage;
import com.turfbook.backend.dto.BulkCreateBookingRequest;
import com.turfbook.backend.dto.CreateBookingRequest;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BookingController implements BookingsApi {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingPage> listBookings(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.listBookings() called - userId={}, status={}, date={}, dateFrom={}",
                principal.getId(), status, date, dateFrom);
        UserEntity currentUser = getUserEntity(principal.getId());
        BookingPage result = bookingService.listBookings(
                currentUser, status, date, dateFrom,
                page != null ? page : 0,
                size != null ? size : 20);
        return ResponseEntity.ok(result);
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<BookingDto> createBooking(CreateBookingRequest request) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.createBooking() called - userId={}, venueId={}, date={}, startTime={}",
                principal.getId(), request.getVenueId(), request.getDate(), request.getStartTime());
        BookingDto dto = bookingService.createBooking(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDto> getBooking(Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.getBooking() called - id={}, userId={}", id, principal.getId());
        UserEntity currentUser = getUserEntity(principal.getId());
        BookingDto dto = bookingService.getBooking(id, currentUser);
        return ResponseEntity.ok(dto);
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<BookingDto> cancelBooking(Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.cancelBooking() called - id={}, userId={}", id, principal.getId());
        BookingDto dto = bookingService.cancelBooking(id, principal.getId());
        return ResponseEntity.ok(dto);
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BookingDto> acceptBooking(Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.acceptBooking() called - id={}, ownerId={}", id, principal.getId());
        return ResponseEntity.ok(bookingService.acceptBooking(id, principal.getId()));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BookingDto> rejectBooking(Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.rejectBooking() called - id={}, ownerId={}", id, principal.getId());
        return ResponseEntity.ok(bookingService.rejectBooking(id, principal.getId()));
    }

    @PatchMapping("/api/v1/bookings/group/{groupId}/cancel")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<List<BookingDto>> cancelBookingGroup(@PathVariable String groupId) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.cancelBookingGroup() called - groupId={}, userId={}", groupId, principal.getId());
        List<BookingDto> results = bookingService.cancelBookingGroup(groupId, principal.getId());
        return ResponseEntity.ok(results);
    }

    @PatchMapping("/api/v1/bookings/{id}/check-in")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BookingDto> checkInBooking(@PathVariable Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.checkInBooking() called - id={}, ownerId={}", id, principal.getId());
        return ResponseEntity.ok(bookingService.checkInBooking(id, principal.getId()));
    }

    @PostMapping("/api/v1/bookings/group/{groupId}/check-in")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BookingDto>> checkInBookingGroup(@PathVariable String groupId) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.checkInBookingGroup() called - groupId={}, ownerId={}", groupId, principal.getId());
        List<BookingDto> results = bookingService.checkInBookingGroup(groupId, principal.getId());
        return ResponseEntity.ok(results);
    }

    private UserPrincipal getCurrentPrincipal() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }

    private UserEntity getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new com.turfbook.backend.exception.ResourceNotFoundException("User", "id", id));
    }
}
