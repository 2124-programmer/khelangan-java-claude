package com.turfbook.backend.controller;

import com.turfbook.backend.api.AuthApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Override
    public ResponseEntity<MessageResponse> sendOtp(OtpSendRequest request) {
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    @Override
    public ResponseEntity<AuthResponse> verifyOtp(OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @Override
    public ResponseEntity<MessageResponse> forgotPassword(ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @Override
    public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
