package com.turfbook.backend.controller;

import com.turfbook.backend.api.AuthApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        log.info("AuthController.register() called");
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        log.info("AuthController.login() called");
        return ResponseEntity.ok(authService.login(request));
    }

    @Override
    public ResponseEntity<OtpSendResponse> sendOtp(OtpSendRequest request) {
        log.info("AuthController.sendOtp() called");
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    @Override
    public ResponseEntity<AuthResponse> verifyOtp(OtpVerifyRequest request) {
        log.info("AuthController.verifyOtp() called");
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @Override
    public ResponseEntity<MessageResponse> forgotPassword(ForgotPasswordRequest request) {
        log.info("AuthController.forgotPassword() called");
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @Override
    public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest request) {
        log.info("AuthController.refreshToken() called");
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @Override
    public ResponseEntity<AuthResponse> googleSignIn(GoogleSignInRequest request) {
        log.info("AuthController.googleSignIn() called");
        return ResponseEntity.ok(authService.googleSignIn(request));
    }
}
