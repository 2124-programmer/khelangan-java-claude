package com.turfbook.backend.controller;

import com.turfbook.backend.api.AdminApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController implements AdminApi {

    private final UserService userService;
    private final SportService sportService;
    private final VenueService venueService;
    private final BookingService bookingService;
    private final CouponService couponService;
    private final PayoutService payoutService;
    private final NotificationService notificationService;
    private final AdminService adminService;

    // ─── Users ────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<UserPage> adminListUsers(Integer page, Integer size, String role, String search) {
        return ResponseEntity.ok(userService.listUsers(
                page != null ? page : 0,
                size != null ? size : 20,
                role,
                search
        ));
    }

    @Override
    public ResponseEntity<UserDto> blockUser(Long id) {
        return ResponseEntity.ok(userService.blockUser(id));
    }

    @Override
    public ResponseEntity<UserDto> unblockUser(Long id) {
        return ResponseEntity.ok(userService.unblockUser(id));
    }

    // ─── Sports ───────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<SportDto> createSport(CreateSportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sportService.createSport(request));
    }

    @Override
    public ResponseEntity<SportDto> updateSport(Long id, UpdateSportRequest request) {
        return ResponseEntity.ok(sportService.updateSport(id, request));
    }

    @Override
    public ResponseEntity<Void> deleteSport(Long id) {
        sportService.deleteSport(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Venues ───────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<VenueSummaryPage> adminListVenues(Integer page, Integer size, String status) {
        return ResponseEntity.ok(venueService.adminListVenues(
                page != null ? page : 0,
                size != null ? size : 20,
                status
        ));
    }

    // ─── Bookings ─────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<BookingPage> adminListBookings(Integer page, Integer size, String status) {
        return ResponseEntity.ok(bookingService.adminListBookings(
                page != null ? page : 0,
                size != null ? size : 20,
                status
        ));
    }

    // ─── Coupons ──────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<CouponPage> adminListCoupons(Integer page, Integer size) {
        return ResponseEntity.ok(couponService.adminListCoupons(
                page != null ? page : 0,
                size != null ? size : 20
        ));
    }

    @Override
    public ResponseEntity<CouponDto> createCoupon(CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @Override
    public ResponseEntity<CouponDto> updateCoupon(Long id, UpdateCouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    // ─── Payouts ──────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<PayoutPage> adminListPayouts(Integer page, Integer size, String status) {
        return ResponseEntity.ok(payoutService.adminListPayouts(
                page != null ? page : 0,
                size != null ? size : 20,
                status
        ));
    }

    @Override
    public ResponseEntity<PayoutDto> processPayout(Long id) {
        return ResponseEntity.ok(payoutService.processPayout(id));
    }

    // ─── Notifications ────────────────────────────────────────────────────

    @Override
    public ResponseEntity<MessageResponse> broadcastNotification(BroadcastRequest request) {
        return ResponseEntity.ok(notificationService.broadcast(request));
    }

    // ─── Stats ────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<AdminStatsDto> getAdminStats() {
        return ResponseEntity.ok(adminService.getAdminStats());
    }

    // ─── Settings ─────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<PlatformSettingsDto> getSettings() {
        return ResponseEntity.ok(adminService.getSettings());
    }

    @Override
    public ResponseEntity<PlatformSettingsDto> updateSettings(UpdateSettingsRequest request) {
        return ResponseEntity.ok(adminService.updateSettings(request));
    }
}
