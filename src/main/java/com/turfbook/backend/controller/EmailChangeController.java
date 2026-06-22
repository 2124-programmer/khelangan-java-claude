package com.turfbook.backend.controller;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.EmailChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/email-change-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EmailChangeRequestDto>> adminList(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(emailChangeService.adminList(status, page, size));
    }

    @PostMapping("/api/v1/admin/email-change-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailChangeRequestDto> adminApprove(@PathVariable Long id) {
        log.info("EmailChangeController.adminApprove() id={}", id);
        return ResponseEntity.ok(emailChangeService.adminApprove(id));
    }

    @PostMapping("/api/v1/admin/email-change-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailChangeRequestDto> adminReject(
            @PathVariable Long id,
            @RequestBody(required = false) EmailChangeRejectRequest request) {
        log.info("EmailChangeController.adminReject() id={}", id);
        if (request == null) request = new EmailChangeRejectRequest();
        return ResponseEntity.ok(emailChangeService.adminReject(id, request));
    }
}
