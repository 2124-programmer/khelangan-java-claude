package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.OtpRecordEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.util.LogMaskUtil;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.UserMapper;
import com.turfbook.backend.repository.OtpRecordRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.JwtTokenProvider;
import com.turfbook.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpRecordRepository otpRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    private static final int OTP_EXPIRY_SEC     = 300; // 5 min
    private static final int OTP_RESEND_SEC     = 60;
    private static final int OTP_MAX_ATTEMPTS   = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        UserEntity.Role role;
        try {
            role = UserEntity.Role.valueOf(request.getRole().name().toUpperCase());
        } catch (Exception e) {
            role = UserEntity.Role.PLAYER;
        }
        // Prevent self-registration as ADMIN
        if (role == UserEntity.Role.ADMIN) {
            role = UserEntity.Role.PLAYER;
        }

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        user = userRepository.save(user);
        log.info("event=USER_REGISTERED userId={} role={}", user.getId(), user.getRole());

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token); // In production, use a separate refresh token store
        response.setUser(userMapper.toDto(user));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("event=LOGIN_ATTEMPT email={}", LogMaskUtil.maskEmail(request.getEmail()));
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserEntity user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isBlocked()) {
            throw new UnauthorizedException("Your account has been blocked. Please contact support.");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name());
        log.info("event=USER_LOGIN userId={} role={}", user.getId(), user.getRole());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token);
        response.setUser(userMapper.toDto(user));
        return response;
    }

    @Override
    @Transactional
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        OtpSendResponse response = new OtpSendResponse();
        response.setExpiresInSec(OTP_EXPIRY_SEC);
        response.setResendAfterSec(0);

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Constant-time response for unknown emails — don't reveal whether account exists.
            log.debug("event=OTP_REQUEST_UNKNOWN_EMAIL");
            response.setMaskedDestination("••••••••");
            return response;
        }
        UserEntity user = userOpt.get();

        // Rate-limit: prevent re-send within the cooldown window.
        Optional<OtpRecordEntity> recent = otpRecordRepository.findTopByEmailOrderByCreatedAtDesc(email);
        if (recent.isPresent()) {
            long secondsAgo = ChronoUnit.SECONDS.between(recent.get().getCreatedAt(), LocalDateTime.now());
            if (secondsAgo < OTP_RESEND_SEC) {
                int remaining = (int) (OTP_RESEND_SEC - secondsAgo);
                log.warn("event=OTP_RATE_LIMITED userId={} resendAfterSec={}", user.getId(), remaining);
                response.setMaskedDestination(maskPhone(user.getPhone()));
                response.setResendAfterSec(remaining);
                return response;
            }
        }

        // Generate, hash, and persist the new OTP.
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String hash = sha256Hex(otp);

        otpRecordRepository.deleteAllByEmail(email);
        OtpRecordEntity otpRecord = OtpRecordEntity.builder()
                .email(email)
                .codeHash(hash)
                .expiresAt(LocalDateTime.now().plusSeconds(OTP_EXPIRY_SEC))
                .build();
        otpRecord = Objects.requireNonNull(otpRecordRepository.save(otpRecord));

        // TODO: Deliver via SMS provider (Twilio / AWS SNS / MSG91) to user.getPhone().
        log.info("event=OTP_REQUESTED userId={} destination={}", user.getId(), maskPhone(user.getPhone()));
        // DEV-ONLY: remove before prod
        log.debug("[DEV] OTP code for userId={} → {}", user.getId(), otp);

        response.setMaskedDestination(maskPhone(user.getPhone()));
        return response;
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        OtpRecordEntity record = otpRecordRepository
                .findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException("OTP has expired or was not requested. Please request a new code."));

        if (record.getAttempts() >= OTP_MAX_ATTEMPTS) {
            record.setUsed(true);
            otpRecordRepository.save(record);
            log.warn("event=OTP_MAX_ATTEMPTS_EXCEEDED email={}", LogMaskUtil.maskEmail(email));
            throw new UnauthorizedException("Too many incorrect attempts. Please request a new code.");
        }

        record.setAttempts(record.getAttempts() + 1);

        if (!sha256Hex(request.getCode()).equals(record.getCodeHash())) {
            otpRecordRepository.save(record);
            int remaining = OTP_MAX_ATTEMPTS - record.getAttempts();
            log.warn("event=OTP_INVALID_CODE email={} attemptsUsed={} remaining={}",
                    LogMaskUtil.maskEmail(email), record.getAttempts(), remaining);
            throw new UnauthorizedException(remaining > 0
                    ? "Incorrect code. " + remaining + " attempt(s) remaining."
                    : "Incorrect code. No attempts remaining — please request a new code.");
        }

        record.setUsed(true);
        otpRecordRepository.save(record);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (user.isBlocked()) {
            throw new UnauthorizedException("Your account has been blocked. Please contact support.");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name());
        log.info("event=OTP_VERIFIED userId={} role={}", user.getId(), user.getRole());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token);
        response.setUser(userMapper.toDto(user));
        return response;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "••••••••";
        int showSuffix = 4;
        int prefixLen = Math.min(3, phone.length() - showSuffix);
        String prefix = phone.substring(0, prefixLen);
        String suffix = phone.substring(phone.length() - showSuffix);
        String dots = "•".repeat(Math.max(0, phone.length() - prefixLen - showSuffix));
        return prefix + dots + suffix;
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        // TODO: Send password reset email
        log.info("Password reset requested for email: {}", request.getEmail());
        MessageResponse response = new MessageResponse();
        response.setMessage("If an account with this email exists, a password reset link has been sent.");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newToken = tokenProvider.generateToken(user.getId(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(newToken);
        response.setRefreshToken(newToken);
        response.setUser(userMapper.toDto(user));
        return response;
    }
}
