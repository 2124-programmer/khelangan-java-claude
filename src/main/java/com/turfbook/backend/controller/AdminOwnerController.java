package com.turfbook.backend.controller;

import com.turfbook.backend.api.AdminOwnersApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.AdminOwnerService;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminOwnerController implements AdminOwnersApi {

    private final AdminOwnerService service;

    @Override
    public ResponseEntity<OwnerPage> listAdminOwners(String q, String segment, String sort, Integer page, Integer size) {
        return ResponseEntity.ok(service.listOwners(q, segment, sort,
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<OwnerStats> getAdminOwnerStats() {
        return ResponseEntity.ok(service.getStats());
    }

    @Override
    public ResponseEntity<OwnerAdminDetail> getOwnerDetail(Long ownerId) {
        return ResponseEntity.ok(service.getDetail(ownerId));
    }

    @Override
    public ResponseEntity<VenueSummaryPage> getOwnerVenues(Long ownerId, Integer page, Integer size) {
        return ResponseEntity.ok(service.getVenues(ownerId, page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<List<VenueSubscriptionRow>> getOwnerSubscriptions(Long ownerId) {
        return ResponseEntity.ok(service.getSubscriptions(ownerId));
    }

    @Override
    public ResponseEntity<OwnerBookingPage> getOwnerBookings(Long ownerId, Integer page, Integer size) {
        return ResponseEntity.ok(service.getBookings(ownerId, page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<PlayerAuditPage> getOwnerAudit(Long ownerId, Integer page, Integer size) {
        return ResponseEntity.ok(service.getAudit(ownerId, page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<OwnerAdminDetail> suspendOwner(Long ownerId, OwnerSuspendBody body) {
        return ResponseEntity.ok(service.suspend(ownerId, body, currentUserId()));
    }

    @Override
    public ResponseEntity<OwnerAdminDetail> reactivateOwner(Long ownerId) {
        return ResponseEntity.ok(service.reactivate(ownerId, currentUserId()));
    }

    @Override
    public ResponseEntity<OwnerAdminDetail> banOwner(Long ownerId, OwnerBanBody body) {
        return ResponseEntity.ok(service.ban(ownerId, body, currentUserId()));
    }

    @Override
    public ResponseEntity<OwnerAdminDetail> unbanOwner(Long ownerId) {
        return ResponseEntity.ok(service.unban(ownerId, currentUserId()));
    }

    @Override
    public ResponseEntity<OwnerAdminDetail> setOwnerVerification(Long ownerId, OwnerVerificationBody body) {
        return ResponseEntity.ok(service.setVerification(ownerId, body, currentUserId()));
    }

    @Override
    public ResponseEntity<Void> forceLogoutOwner(Long ownerId) {
        service.forceLogout(ownerId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> triggerOwnerPasswordReset(Long ownerId) {
        service.triggerPasswordReset(ownerId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> messageOwner(Long ownerId, OwnerMessageBody body) {
        service.message(ownerId, body, currentUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Override
    public ResponseEntity<OwnerAdminDetail> deleteOwner(Long ownerId, OwnerReasonBody body) {
        return ResponseEntity.ok(service.delete(ownerId, body, currentUserId()));
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof UserPrincipal up ? up.getId() : null;
    }
}
