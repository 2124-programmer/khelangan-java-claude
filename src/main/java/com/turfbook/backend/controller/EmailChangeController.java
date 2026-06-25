package com.turfbook.backend.controller;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.EmailChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EmailChangeController {

    private final EmailChangeService emailChangeService;

    // ── Owner endpoints ───────────────────────────────────────────────────────

    @PostMapping("/api/v1/owner/email-change-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> createRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmailChangeCreateRequest request) {
        log.info("EmailChangeController.createRequest() userId={}", principal.getId());
        return ResponseEntity.ok(emailChangeService.createRequest(principal.getId(), request));
    }

    @PostMapping("/api/v1/owner/email-change-requests/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmailChangeRequestDto> verifyOtp(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmailChangeVerifyRequest request) {
        log.info("EmailChangeController.verifyOtp() userId={}", principal.getId());
        return ResponseEntity.ok(emailChangeService.verifyOtp(principal.getId(), request));
    }

    @GetMapping("/api/v1/owner/email-change-requests/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmailChangeRequestDto> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("EmailChangeController.getStatus() userId={}", principal.getId());
        EmailChangeRequestDto status = emailChangeService.getStatus(principal.getId());
        if (status == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(status);
    }

    // Email change is fully self-service: a verified OTP applies the change immediately
    // (see EmailChangeService.verifyOtp), so there is no admin review/approve/reject path.
}
