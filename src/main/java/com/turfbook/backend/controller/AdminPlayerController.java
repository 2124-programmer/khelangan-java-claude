package com.turfbook.backend.controller;

import com.turfbook.backend.api.AdminPlayersApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.AdminPlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlayerController implements AdminPlayersApi {

    private final AdminPlayerService service;

    @Override
    public ResponseEntity<PlayerPage> listAdminPlayers(String q, String segment, String sort, Integer page, Integer size) {
        return ResponseEntity.ok(service.listPlayers(q, segment, sort,
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<PlayerStats> getPlayerStats() {
        return ResponseEntity.ok(service.getStats());
    }

    @Override
    public ResponseEntity<PlayerAdminDetail> getPlayerDetail(Long playerId) {
        return ResponseEntity.ok(service.getDetail(playerId));
    }

    @Override
    public ResponseEntity<PlayerAdminDetail> suspendPlayer(Long playerId, PlayerSuspendBody body) {
        return ResponseEntity.ok(service.suspend(playerId, body, currentUserId()));
    }

    @Override
    public ResponseEntity<PlayerAdminDetail> reactivatePlayer(Long playerId) {
        return ResponseEntity.ok(service.reactivate(playerId, currentUserId()));
    }

    @Override
    public ResponseEntity<PlayerAdminDetail> banPlayer(Long playerId, PlayerReasonBody body) {
        return ResponseEntity.ok(service.ban(playerId, body, currentUserId()));
    }

    @Override
    public ResponseEntity<PlayerAdminDetail> unbanPlayer(Long playerId) {
        return ResponseEntity.ok(service.unban(playerId, currentUserId()));
    }

    /** Soft-delete a player (SUPER_ADMIN only). Hand-written — not part of the generated contract. */
    @DeleteMapping("/api/v1/admin/players/{playerId}")
    public ResponseEntity<PlayerAdminDetail> deletePlayer(
            @PathVariable Long playerId, @Valid @RequestBody PlayerReasonBody body) {
        return ResponseEntity.ok(service.delete(playerId, body, currentUserId()));
    }

    @Override
    public ResponseEntity<PlayerAdminDetail> setPlayerVerification(Long playerId, PlayerVerificationBody body) {
        return ResponseEntity.ok(service.setVerification(playerId, body, currentUserId()));
    }

    @Override
    public ResponseEntity<Void> forceLogoutPlayer(Long playerId) {
        service.forceLogout(playerId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> triggerPlayerPasswordReset(Long playerId) {
        service.triggerPasswordReset(playerId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> messagePlayer(Long playerId, PlayerMessageBody body) {
        service.message(playerId, body, currentUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Override
    public ResponseEntity<PlayerBookingPage> getPlayerBookings(Long playerId, Integer page, Integer size) {
        return ResponseEntity.ok(service.getBookings(playerId, page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<PlayerPaymentPage> getPlayerPayments(Long playerId, Integer page, Integer size) {
        return ResponseEntity.ok(service.getPayments(playerId, page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    public ResponseEntity<PlayerAuditPage> getPlayerAudit(Long playerId, Integer page, Integer size) {
        return ResponseEntity.ok(service.getAudit(playerId, page != null ? page : 0, size != null ? size : 20));
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof UserPrincipal up ? up.getId() : null;
    }
}
