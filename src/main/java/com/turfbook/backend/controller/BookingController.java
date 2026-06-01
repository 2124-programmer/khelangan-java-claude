package com.turfbook.backend.controller;

import com.turfbook.backend.api.BookingsApi;
import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.dto.BookingPage;
import com.turfbook.backend.dto.CreateBookingRequest;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookingController implements BookingsApi {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingPage> listBookings(
            String status,
            Integer page,
            Integer size) {

        UserPrincipal principal = getCurrentPrincipal();
        UserEntity currentUser = getUserEntity(principal.getId());

        BookingPage result = bookingService.listBookings(
                currentUser,
                status,
                page != null ? page : 0,
                size != null ? size : 20
        );
        return ResponseEntity.ok(result);
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<BookingDto> createBooking(CreateBookingRequest request) {
        UserPrincipal principal = getCurrentPrincipal();
        BookingDto dto = bookingService.createBooking(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDto> getBooking(Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        UserEntity currentUser = getUserEntity(principal.getId());
        BookingDto dto = bookingService.getBooking(id, currentUser);
        return ResponseEntity.ok(dto);
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<BookingDto> cancelBooking(Long id) {
        UserPrincipal principal = getCurrentPrincipal();
        BookingDto dto = bookingService.cancelBooking(id, principal.getId());
        return ResponseEntity.ok(dto);
    }

    // ─── Admin endpoint (mapped from AdminApi or handled here via AdminController) ─────
    // Note: adminListBookings is in AdminController since it's under /api/v1/admin/bookings

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
