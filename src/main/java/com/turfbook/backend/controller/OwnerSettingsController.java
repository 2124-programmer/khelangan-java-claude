package com.turfbook.backend.controller;

import com.turfbook.backend.api.OwnerSettingsApi;
import com.turfbook.backend.dto.OwnerSettingsDto;
import com.turfbook.backend.dto.UpdateOwnerSettingsRequest;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.OwnerSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OwnerSettingsController implements OwnerSettingsApi {

    private final OwnerSettingsService ownerSettingsService;

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerSettingsDto> getOwnerSettings() {
        Long ownerId = getPrincipal().getId();
        log.info("OwnerSettingsController.getOwnerSettings() - ownerId={}", ownerId);
        return ResponseEntity.ok(ownerSettingsService.getSettings(ownerId));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerSettingsDto> updateOwnerSettings(UpdateOwnerSettingsRequest request) {
        Long ownerId = getPrincipal().getId();
        log.info("OwnerSettingsController.updateOwnerSettings() - ownerId={}", ownerId);
        return ResponseEntity.ok(ownerSettingsService.updateSettings(ownerId, request));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
