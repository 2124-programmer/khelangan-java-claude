package com.turfbook.backend.service;

import com.turfbook.backend.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    MessageResponse sendOtp(OtpSendRequest request);

    AuthResponse verifyOtp(OtpVerifyRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}
