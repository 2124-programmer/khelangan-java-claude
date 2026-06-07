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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BookingController implements BookingsApi {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingPage> listBookings(String status, Integer page, Integer size) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.listBookings() called - userId={}, status={}", principal.getId(), status);
        UserEntity currentUser = getUserEntity(principal.getId());
        BookingPage result = bookingService.listBookings(
                currentUser, status,
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

    @PostMapping("/api/v1/bookings/bulk")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<List<BookingDto>> bulkCreateBookings(@RequestBody BulkCreateBookingRequest request) {
        UserPrincipal principal = getCurrentPrincipal();
        log.info("BookingController.bulkCreateBookings() called - userId={}, courtId={}, date={}, count={}",
                principal.getId(), request.getCourtId(), request.getDate(),
                request.getStartTimes() == null ? 0 : request.getStartTimes().size());
        List<BookingDto> dtos = bookingService.bulkCreateBookings(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
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
