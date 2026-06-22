package com.turfbook.backend.controller;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Standalone controller for authenticated security actions (change password)
 * and unauthenticated password-reset via email OTP.
 * Not wired to the generated AuthApi interface — avoids regeneration conflicts.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthSecurityController {

    private final AuthService authService;

    // ── Change password (authenticated) ──────────────────────────────────────

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("AuthSecurityController.changePassword() userId={}", principal.getId());
        return ResponseEntity.ok(authService.changePassword(principal.getId(), request));
    }

    // ── Password reset via email OTP (public / unauthenticated) ──────────────

    @PostMapping("/password-reset/request")
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        log.info("AuthSecurityController.requestPasswordReset() called");
        return ResponseEntity.ok(authService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<PasswordResetVerifyResponse> verifyPasswordResetOtp(
            @Valid @RequestBody PasswordResetVerifyRequest request) {
        log.info("AuthSecurityController.verifyPasswordResetOtp() called");
        return ResponseEntity.ok(authService.verifyPasswordResetOtp(request));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<MessageResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        log.info("AuthSecurityController.confirmPasswordReset() called");
        return ResponseEntity.ok(authService.confirmPasswordReset(request));
    }
}
