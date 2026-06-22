package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.OtpRecordEntity;
import com.turfbook.backend.entity.OtpRecordEntity.Purpose;
import com.turfbook.backend.entity.PasswordResetTokenEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.TooManyRequestsException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.UserMapper;
import com.turfbook.backend.repository.OtpRecordRepository;
import com.turfbook.backend.repository.PasswordResetTokenRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.JwtTokenProvider;
import com.turfbook.backend.service.AuthService;
import com.turfbook.backend.service.MailService;
import com.turfbook.backend.util.HashUtil;
import com.turfbook.backend.util.LogMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpRecordRepository otpRecordRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final MailService mailService;

    private static final int OTP_EXPIRY_SEC       = 300; // 5 min
    private static final int OTP_RESEND_SEC        = 60;
    private static final int OTP_MAX_ATTEMPTS      = 5;
    private static final int OTP_MAX_PER_HOUR      = 5;
    private static final int RESET_TOKEN_EXPIRY_MIN = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Simple in-memory rate limiter for change-password (per userId, hourly window)
    private static final ConcurrentHashMap<Long, AtomicInteger> cpAttempts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> cpWindowStart = new ConcurrentHashMap<>();
    private static final int CP_MAX_PER_HOUR = 10;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("AuthService.register() called - email={}", LogMaskUtil.maskEmail(request.getEmail()));
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        UserEntity.Role role;
        try {
            role = UserEntity.Role.valueOf(request.getRole().name().toUpperCase());
        } catch (Exception e) {
            role = UserEntity.Role.PLAYER;
        }
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

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name(), user.getTokenVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token);
        response.setUser(userMapper.toDto(user));
        log.info("AuthService.register() completed - userId={}, role={}", user.getId(), user.getRole());
        return response;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("AuthService.login() called - email={}", LogMaskUtil.maskEmail(request.getEmail()));
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

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name(), user.getTokenVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token);
        response.setUser(userMapper.toDto(user));
        log.info("AuthService.login() completed - userId={}, role={}", user.getId(), user.getRole());
        return response;
    }

    // ── OTP login ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("AuthService.sendOtp() called - email={}", LogMaskUtil.maskEmail(email));
        OtpSendResponse response = new OtpSendResponse();
        response.setExpiresInSec(OTP_EXPIRY_SEC);
        response.setResendAfterSec(0);

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("AuthService.sendOtp() - no account found for email={}", LogMaskUtil.maskEmail(email));
            response.setMaskedDestination("••••••••");
            return response;
        }
        UserEntity user = userOpt.get();

        Optional<OtpRecordEntity> recent = otpRecordRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, Purpose.AUTH_OTP);
        if (recent.isPresent()) {
            long secondsAgo = ChronoUnit.SECONDS.between(recent.get().getCreatedAt(), LocalDateTime.now());
            if (secondsAgo < OTP_RESEND_SEC) {
                int remaining = (int) (OTP_RESEND_SEC - secondsAgo);
                response.setMaskedDestination(maskPhone(user.getPhone()));
                response.setResendAfterSec(remaining);
                return response;
            }
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String hash = HashUtil.sha256Hex(otp);

        otpRecordRepository.deleteAllByEmailAndPurpose(email, Purpose.AUTH_OTP);
        OtpRecordEntity otpRecord = OtpRecordEntity.builder()
                .email(email)
                .purpose(Purpose.AUTH_OTP)
                .codeHash(hash)
                .expiresAt(LocalDateTime.now().plusSeconds(OTP_EXPIRY_SEC))
                .build();
        otpRecordRepository.save(Objects.requireNonNull(otpRecord));

        log.info("[DEV] OTP for {} → {} (remove before prod)", maskPhone(user.getPhone()), otp);

        response.setMaskedDestination(maskPhone(user.getPhone()));
        return response;
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("AuthService.verifyOtp() called - email={}", LogMaskUtil.maskEmail(email));

        OtpRecordEntity record = otpRecordRepository
                .findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        email, Purpose.AUTH_OTP, LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException(
                        "OTP has expired or was not requested. Please request a new code."));

        if (record.getAttempts() >= OTP_MAX_ATTEMPTS) {
            record.setUsed(true);
            otpRecordRepository.save(record);
            throw new UnauthorizedException("Too many incorrect attempts. Please request a new code.");
        }

        record.setAttempts(record.getAttempts() + 1);

        if (!HashUtil.sha256Hex(request.getCode()).equals(record.getCodeHash())) {
            otpRecordRepository.save(record);
            int remaining = OTP_MAX_ATTEMPTS - record.getAttempts();
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

        String token = tokenProvider.generateToken(user.getId(), user.getRole().name(), user.getTokenVersion());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(token);
        response.setUser(userMapper.toDto(user));
        return response;
    }

    // ── Forgot password (legacy stub — kept for backwards-compat) ─────────────

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        // Delegates to the new OTP-based reset flow (enumeration-safe)
        return requestPasswordReset(new PasswordResetRequest() {{
            setEmail(request.getEmail());
        }});
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("AuthService.refreshToken() called");
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newToken = tokenProvider.generateToken(user.getId(), user.getRole().name(), user.getTokenVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(newToken);
        response.setRefreshToken(newToken);
        response.setUser(userMapper.toDto(user));
        return response;
    }

    // ── Change password (authenticated) ──────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest request) {
        log.info("AuthService.changePassword() called - userId={}", userId);
        enforceChangePasswordRateLimit(userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }

        validatePasswordPolicy(request.getNewPassword());

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must differ from the current one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        // Issue fresh token for the current device
        String newToken = tokenProvider.generateToken(user.getId(), user.getRole().name(), user.getTokenVersion());
        AuthResponse response = new AuthResponse();
        response.setToken(newToken);
        response.setRefreshToken(newToken);
        response.setUser(userMapper.toDto(user));
        log.info("AuthService.changePassword() completed - userId={}", userId);
        return response;
    }

    // ── Password reset via email OTP ──────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse requestPasswordReset(PasswordResetRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("AuthService.requestPasswordReset() called - email={}", LogMaskUtil.maskEmail(email));

        MessageResponse generic = new MessageResponse();
        generic.setMessage("If an account with this email exists, a reset code has been sent.");

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return generic; // enumeration-safe: same response regardless
        }

        // Per-hour send cap
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long sentThisHour = otpRecordRepository.countByEmailAndPurposeAndCreatedAtAfter(
                email, Purpose.PASSWORD_RESET, oneHourAgo);
        if (sentThisHour >= OTP_MAX_PER_HOUR) {
            return generic; // rate-limited but still generic response
        }

        // Cooldown
        Optional<OtpRecordEntity> recent = otpRecordRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, Purpose.PASSWORD_RESET);
        if (recent.isPresent()) {
            long secondsAgo = ChronoUnit.SECONDS.between(recent.get().getCreatedAt(), LocalDateTime.now());
            if (secondsAgo < OTP_RESEND_SEC) {
                return generic; // cooldown — still generic response
            }
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        otpRecordRepository.deleteAllByEmailAndPurpose(email, Purpose.PASSWORD_RESET);
        OtpRecordEntity record = OtpRecordEntity.builder()
                .email(email)
                .purpose(Purpose.PASSWORD_RESET)
                .codeHash(HashUtil.sha256Hex(otp))
                .expiresAt(LocalDateTime.now().plusSeconds(OTP_EXPIRY_SEC))
                .build();
        otpRecordRepository.save(record);

        mailService.sendPasswordResetOtp(email, otp, OTP_EXPIRY_SEC / 60);
        log.info("AuthService.requestPasswordReset() - OTP sent to email={}", LogMaskUtil.maskEmail(email));
        return generic;
    }

    @Override
    @Transactional
    public PasswordResetVerifyResponse verifyPasswordResetOtp(PasswordResetVerifyRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("AuthService.verifyPasswordResetOtp() called - email={}", LogMaskUtil.maskEmail(email));

        OtpRecordEntity record = otpRecordRepository
                .findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        email, Purpose.PASSWORD_RESET, LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException(
                        "Reset code has expired or was not requested. Please request a new code."));

        if (record.getAttempts() >= OTP_MAX_ATTEMPTS) {
            record.setUsed(true);
            otpRecordRepository.save(record);
            throw new UnauthorizedException("Too many incorrect attempts. Please request a new code.");
        }

        record.setAttempts(record.getAttempts() + 1);

        if (!HashUtil.sha256Hex(request.getOtp()).equals(record.getCodeHash())) {
            otpRecordRepository.save(record);
            int remaining = OTP_MAX_ATTEMPTS - record.getAttempts();
            throw new UnauthorizedException(remaining > 0
                    ? "Incorrect code. " + remaining + " attempt(s) remaining."
                    : "Incorrect code. No attempts remaining — please request a new code.");
        }

        record.setUsed(true);
        otpRecordRepository.save(record);

        // Issue a short-lived, single-use reset token
        String rawToken = generateSecureHex(32);
        passwordResetTokenRepository.deleteAllByUserId(
                userRepository.findByEmail(email).map(UserEntity::getId).orElse(-1L));

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        PasswordResetTokenEntity tokenEntity = PasswordResetTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(HashUtil.sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MIN))
                .build();
        passwordResetTokenRepository.save(tokenEntity);

        log.info("AuthService.verifyPasswordResetOtp() - reset token issued for userId={}", user.getId());
        return new PasswordResetVerifyResponse(rawToken);
    }

    @Override
    @Transactional
    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        log.info("AuthService.confirmPasswordReset() called");

        PasswordResetTokenEntity tokenEntity = passwordResetTokenRepository
                .findByTokenHashAndUsedFalseAndExpiresAtAfter(
                        HashUtil.sha256Hex(request.getResetToken()), LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException(
                        "Reset token is invalid or has expired. Please restart the reset flow."));

        validatePasswordPolicy(request.getNewPassword());

        UserEntity user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Account not found."));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must differ from the current one.");
        }

        tokenEntity.setUsed(true);
        passwordResetTokenRepository.save(tokenEntity);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("AuthService.confirmPasswordReset() completed - userId={}", user.getId());
        MessageResponse response = new MessageResponse();
        response.setMessage("Password reset successful. Please log in with your new password.");
        return response;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String generateSecureHex(int bytes) {
        byte[] buf = new byte[bytes];
        SECURE_RANDOM.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
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

    private static void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BadRequestException("Password must contain at least one letter and one digit.");
        }
    }

    private void enforceChangePasswordRateLimit(Long userId) {
        long now = System.currentTimeMillis();
        long windowMs = 3_600_000L; // 1 hour

        cpWindowStart.putIfAbsent(userId, now);
        cpAttempts.putIfAbsent(userId, new AtomicInteger(0));

        long windowStart = cpWindowStart.get(userId);
        if (now - windowStart > windowMs) {
            cpWindowStart.put(userId, now);
            cpAttempts.get(userId).set(0);
        }

        int count = cpAttempts.get(userId).incrementAndGet();
        if (count > CP_MAX_PER_HOUR) {
            throw new TooManyRequestsException("Too many password change attempts. Please try again later.");
        }
    }
}
