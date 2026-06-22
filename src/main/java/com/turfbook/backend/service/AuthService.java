package com.turfbook.backend.service;

import com.turfbook.backend.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    OtpSendResponse sendOtp(OtpSendRequest request);

    AuthResponse verifyOtp(OtpVerifyRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    // ── New: change password (authenticated) ─────────────────────────────────

    AuthResponse changePassword(Long userId, ChangePasswordRequest request);

    // ── New: password-reset via email OTP (public / unauthenticated) ─────────

    MessageResponse requestPasswordReset(PasswordResetRequest request);

    PasswordResetVerifyResponse verifyPasswordResetOtp(PasswordResetVerifyRequest request);

    MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request);
}
