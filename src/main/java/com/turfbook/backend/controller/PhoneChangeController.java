package com.turfbook.backend.controller;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.PhoneChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service phone change (role-neutral — players and owners). A verified OTP applies the change
 * immediately; there is no admin review. Mirrors {@link EmailChangeController}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PhoneChangeController {

    private final PhoneChangeService phoneChangeService;

    @PostMapping("/api/v1/users/me/phone-change-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> createRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PhoneChangeCreateRequest request) {
        log.info("PhoneChangeController.createRequest() userId={}", principal.getId());
        return ResponseEntity.ok(phoneChangeService.createRequest(principal.getId(), request));
    }

    @PostMapping("/api/v1/users/me/phone-change-requests/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PhoneChangeRequestDto> verifyOtp(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PhoneChangeVerifyRequest request) {
        log.info("PhoneChangeController.verifyOtp() userId={}", principal.getId());
        return ResponseEntity.ok(phoneChangeService.verifyOtp(principal.getId(), request));
    }

    @GetMapping("/api/v1/users/me/phone-change-requests/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PhoneChangeRequestDto> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("PhoneChangeController.getStatus() userId={}", principal.getId());
        PhoneChangeRequestDto status = phoneChangeService.getStatus(principal.getId());
        if (status == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(status);
    }
}
