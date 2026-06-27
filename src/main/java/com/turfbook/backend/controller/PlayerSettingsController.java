package com.turfbook.backend.controller;

import com.turfbook.backend.dto.PlayerSettingsDto;
import com.turfbook.backend.dto.UpdatePlayerSettingsRequest;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.PlayerSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Per-player preferences. Available to any authenticated user (a player keeps these even after a
 * role switch). Mirrors {@link OwnerSettingsController}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PlayerSettingsController {

    private final PlayerSettingsService playerSettingsService;

    @GetMapping("/api/v1/player/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlayerSettingsDto> getPlayerSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("PlayerSettingsController.getPlayerSettings() - playerId={}", principal.getId());
        return ResponseEntity.ok(playerSettingsService.getSettings(principal.getId()));
    }

    @PutMapping("/api/v1/player/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlayerSettingsDto> updatePlayerSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePlayerSettingsRequest request) {
        log.info("PlayerSettingsController.updatePlayerSettings() - playerId={}", principal.getId());
        return ResponseEntity.ok(playerSettingsService.updateSettings(principal.getId(), request));
    }
}
