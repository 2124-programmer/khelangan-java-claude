package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.OtpRecordEntity;
import com.turfbook.backend.entity.OtpRecordEntity.Purpose;
import com.turfbook.backend.entity.PasswordResetTokenEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ForbiddenException;
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
import org.springframework.beans.factory.annotation.Value;
import com.turfbook.backend.util.LogMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
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

    private static final int OTP_EXPIRY_SEC       = 600; // 10 min
    private static final int OTP_RESEND_SEC        = 45;
    private static final int OTP_MAX_ATTEMPTS      = 5;
    private static final int OTP_MAX_PER_HOUR      = 5;
    private static final int RESET_TOKEN_EXPIRY_MIN = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** DEV ONLY (app.otp.debug-log-codes): logs generated OTP codes so the flow can be tested without an inbox. */
    @Value("${app.otp.debug-log-codes:false}")
    private boolean otpDebugLogCodes;

    // Simple in-memory rate limiter for change-password (per userId, hourly window)
    private static final ConcurrentHashMap<Long, AtomicInteger> cpAttempts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> cpWindowStart = new ConcurrentHashMap<>();
    private static final int CP_MAX_PER_HOUR = 10;

    // Login throttle: lock an email after N failed credential attempts within a rolling window.
    private static final ConcurrentHashMap<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> loginWindowStart = new ConcurrentHashMap<>();
    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final long LOGIN_WINDOW_MS = 900_000L; // 15 min

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("AuthService.register() called - email={}", LogMaskUtil.maskEmail(request.getEmail()));
        String email = request.getEmail().toLowerCase().trim();
        // Normalize the phone (strip spaces/dashes) so uniqueness + active_phone match how the
        // phone-change flow stores it — otherwise "98765 43210" and "9876543210" could both register.
        String phone = normalizePhone(request.getPhone());

        // Terms & Privacy consent is mandatory (DPDP) — reject if not explicitly accepted.
        if (!Boolean.TRUE.equals(request.getAcceptedTerms())) {
            throw new BadRequestException("You must accept the Terms & Privacy Policy to create an account.");
        }
        // Enforce the shared password policy (≥8 chars, at least one letter and one digit).
        validatePasswordPolicy(request.getPassword());

        // Uniqueness is enforced against any LIVE (non-deleted) account that already holds this
        // email/phone — matched on the raw column so it holds even if a legacy row never had its
        // active_* backfilled. A DELETED account is excluded, so it frees its identifier for reuse.
        if (userRepository.isEmailInUseByLiveAccount(email, UserEntity.AccountStatus.DELETED)) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }
        if (phone != null && !phone.isEmpty()
                && userRepository.isPhoneInUseByLiveAccount(phone, UserEntity.AccountStatus.DELETED)) {
            throw new ConflictException("Phone number already registered: " + request.getPhone());
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
                .email(email)
                .phone(request.getPhone())
                .activeEmail(email)
                .activePhone(phone != null && !phone.isEmpty() ? phone : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .phoneVerified(false) // phone verification deferred; set true later when OTP-verified
                .acceptedTermsAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        AuthResponse response = new AuthResponse();
        issueTokens(response, user);
        response.setUser(userMapper.toDto(user));
        log.info("AuthService.register() completed - userId={}, role={}", user.getId(), user.getRole());
        return response;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("AuthService.login() called - email={}", LogMaskUtil.maskEmail(request.getEmail()));
        String email = request.getEmail().toLowerCase().trim();
        // Throttle brute-force: a locked email is rejected before we even hit the auth manager.
        assertLoginNotLocked(email);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (LockedException | DisabledException e) {
            // A moderated (suspended/banned) account has accountNonLocked/enabled = false, so Spring
            // throws here before our own block-check. Translate it into a clear, status-aware message
            // (a ForbiddenException → 403, which also avoids the 401 refresh interceptor on the client).
            throw new ForbiddenException(blockedMessageFor(email));
        } catch (BadCredentialsException e) {
            // Count the failure (generic message — never reveal which field was wrong).
            recordLoginFailure(email);
            throw new UnauthorizedException("Invalid email or password");
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        clearLoginFailures(email); // successful auth resets the counter

        UserEntity user = userRepository.findByActiveEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isBlocked()) {
            throw new ForbiddenException(blockedMessageFor(email));
        }

        AuthResponse response = new AuthResponse();
        issueTokens(response, user);
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

        Optional<UserEntity> userOpt = userRepository.findByActiveEmail(email);
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

        UserEntity user = userRepository.findByActiveEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (user.isBlocked()) {
            throw new ForbiddenException(blockedMessageFor(email));
        }

        AuthResponse response = new AuthResponse();
        issueTokens(response, user);
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
        // An access token must never be replayable at the refresh endpoint. Legacy tokens
        // (issued before token typing, type == null) are still accepted for a smooth migration.
        if (JwtTokenProvider.TYPE_ACCESS.equals(tokenProvider.getTokenType(refreshToken))) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        // Refresh tokens are revoked when the session is bumped (password change/reset, logout).
        if (tokenProvider.getTokenVersionFromToken(refreshToken) != user.getTokenVersion()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        // Rotation: every refresh mints a brand-new access + refresh pair.
        AuthResponse response = new AuthResponse();
        issueTokens(response, user);
        response.setUser(userMapper.toDto(user));
        return response;
    }

    /** Issue a distinct short-lived access token and long-lived refresh token onto the response. */
    private void issueTokens(AuthResponse response, UserEntity user) {
        response.setToken(tokenProvider.generateToken(
                user.getId(), user.getRole().name(), user.getTokenVersion()));
        response.setRefreshToken(tokenProvider.generateRefreshToken(
                user.getId(), user.getRole().name(), user.getTokenVersion()));
        // Forced first-login password change signal (e.g. bootstrap-seeded super-admin). The client
        // routes to the change-password screen; the server also blocks other endpoints until cleared.
        response.setMustChangePassword(user.isMustChangePassword());
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
        // Clear the forced-change flag: a successful change satisfies the first-login requirement.
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Issue a fresh access + refresh pair for the current device (old JWT is now invalidated
        // by the tokenVersion bump above, so the client must use these new tokens).
        AuthResponse response = new AuthResponse();
        issueTokens(response, user);
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

        Optional<UserEntity> userOpt = userRepository.findByActiveEmail(email);
        if (userOpt.isEmpty()) {
            // Logged (server-side) for debugging; the response stays identical (enumeration-safe).
            log.info("requestPasswordReset - no account for email={}, sending no mail (neutral response)",
                    LogMaskUtil.maskEmail(email));
            return generic;
        }

        // Per-hour send cap
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long sentThisHour = otpRecordRepository.countByEmailAndPurposeAndCreatedAtAfter(
                email, Purpose.PASSWORD_RESET, oneHourAgo);
        if (sentThisHour >= OTP_MAX_PER_HOUR) {
            log.info("requestPasswordReset - hourly cap reached ({}/{}) for email={}, skipping send",
                    sentThisHour, OTP_MAX_PER_HOUR, LogMaskUtil.maskEmail(email));
            return generic; // rate-limited but still generic response
        }

        // Cooldown
        Optional<OtpRecordEntity> recent = otpRecordRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, Purpose.PASSWORD_RESET);
        if (recent.isPresent()) {
            long secondsAgo = ChronoUnit.SECONDS.between(recent.get().getCreatedAt(), LocalDateTime.now());
            if (secondsAgo < OTP_RESEND_SEC) {
                log.info("requestPasswordReset - cooldown active ({}s of {}s) for email={}, skipping send",
                        secondsAgo, OTP_RESEND_SEC, LogMaskUtil.maskEmail(email));
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

        if (otpDebugLogCodes) {
            log.warn("[DEV] password reset OTP for email={} -> {} (disable app.otp.debug-log-codes in prod)",
                    LogMaskUtil.maskEmail(email), otp);
        }

        // Async dispatch — actual SMTP success/failure is logged by MailService.
        mailService.sendPasswordResetOtp(email, otp, OTP_EXPIRY_SEC / 60);
        log.info("requestPasswordReset - reset email dispatched (async) for userId={}, email={}",
                userOpt.get().getId(), LogMaskUtil.maskEmail(email));
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
            log.info("verifyPasswordResetOtp - too many attempts ({}) for email={}, code locked",
                    record.getAttempts(), LogMaskUtil.maskEmail(email));
            throw new UnauthorizedException("Too many incorrect attempts. Please request a new code.");
        }

        record.setAttempts(record.getAttempts() + 1);

        if (!HashUtil.sha256Hex(request.getOtp()).equals(record.getCodeHash())) {
            otpRecordRepository.save(record);
            int remaining = OTP_MAX_ATTEMPTS - record.getAttempts();
            log.info("verifyPasswordResetOtp - incorrect code for email={} (attempt {}/{}, {} left)",
                    LogMaskUtil.maskEmail(email), record.getAttempts(), OTP_MAX_ATTEMPTS, remaining);
            throw new UnauthorizedException(remaining > 0
                    ? "Incorrect code. " + remaining + " attempt(s) remaining."
                    : "Incorrect code. No attempts remaining — please request a new code.");
        }

        record.setUsed(true);
        otpRecordRepository.save(record);

        // Issue a short-lived, single-use reset token
        String rawToken = generateSecureHex(32);
        passwordResetTokenRepository.deleteAllByUserId(
                userRepository.findByActiveEmail(email).map(UserEntity::getId).orElse(-1L));

        UserEntity user = userRepository.findByActiveEmail(email)
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

    /** A human, status-aware reason for a blocked login (suspended vs banned vs generic). */
    private String blockedMessageFor(String email) {
        UserEntity u = userRepository.findByActiveEmail(email).orElse(null);
        if (u == null) return "Your account has been blocked. Please contact support.";
        return switch (u.getStatus()) {
            case SUSPENDED -> "Your account has been suspended."
                    + (u.getSuspendedReason() != null ? " Reason: " + u.getSuspendedReason() : "")
                    + " Please contact support.";
            case BANNED -> "Your account has been banned. Please contact support.";
            case DELETED -> "This account no longer exists.";
            default -> "Your account has been blocked. Please contact support.";
        };
    }

    /** Trim + strip spaces/dashes so phone uniqueness is consistent with the phone-change flow. */
    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[\\s-]", "").trim();
    }

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

    /** Reject login if this email has exceeded the failed-attempt budget; surfaces a retry-after. */
    private void assertLoginNotLocked(String email) {
        Long start = loginWindowStart.get(email);
        AtomicInteger attempts = loginAttempts.get(email);
        if (start == null || attempts == null) return;
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > LOGIN_WINDOW_MS) { // window expired → forget
            clearLoginFailures(email);
            return;
        }
        if (attempts.get() >= LOGIN_MAX_ATTEMPTS) {
            long retryAfter = (LOGIN_WINDOW_MS - elapsed + 999) / 1000;
            throw new TooManyRequestsException("Too many attempts. Try again in " + retryAfter + "s.");
        }
    }

    private void recordLoginFailure(String email) {
        long now = System.currentTimeMillis();
        Long start = loginWindowStart.get(email);
        if (start == null || now - start > LOGIN_WINDOW_MS) {
            loginWindowStart.put(email, now);
            loginAttempts.put(email, new AtomicInteger(0));
        }
        loginAttempts.computeIfAbsent(email, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private void clearLoginFailures(String email) {
        loginAttempts.remove(email);
        loginWindowStart.remove(email);
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
