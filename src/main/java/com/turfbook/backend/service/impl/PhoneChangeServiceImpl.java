package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.PhoneChangeRequestEntity;
import com.turfbook.backend.entity.PhoneChangeRequestEntity.Status;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.TooManyRequestsException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.repository.PhoneChangeRequestRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.MailService;
import com.turfbook.backend.service.PhoneChangeService;
import com.turfbook.backend.util.HashUtil;
import com.turfbook.backend.util.LogMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Self-service phone-change OTP flow — the SMS analogue of {@link EmailChangeServiceImpl}.
 * Mirrors its OTP lifecycle (generate → hash → verify → apply), enforces {@code active_phone}
 * uniqueness on both request and verify, and keeps {@code phone}/{@code activePhone} in sync.
 *
 * <p>The verification code is delivered over email (the platform's existing OTP channel) to the
 * account's registered address — so a phone change is authorised by the verified account holder
 * without requiring an SMS gateway.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneChangeServiceImpl implements PhoneChangeService {

    private final UserRepository userRepository;
    private final PhoneChangeRequestRepository phoneChangeRepo;
    private final MailService mailService;

    private static final int OTP_EXPIRY_SEC   = 300;
    private static final int OTP_RESEND_SEC   = 60;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final List<Status> UNRESOLVED_STATUSES = List.of(Status.PENDING_VERIFICATION);

    @Override
    @Transactional
    public MessageResponse createRequest(Long userId, PhoneChangeCreateRequest request) {
        String newPhone = normalize(request.getNewPhone());
        log.info("PhoneChangeService.createRequest() userId={}, newPhone={}",
                userId, LogMaskUtil.maskPhone(newPhone));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (newPhone.equals(normalize(user.getPhone()))) {
            throw new BadRequestException("New phone is the same as your current phone.");
        }

        // Block if any LIVE (non-deleted) account already holds this number — matched on the raw
        // column (normalized in-query) so a legacy row with a NULL active_phone still collides.
        if (userRepository.isPhoneInUseByLiveAccount(newPhone, UserEntity.AccountStatus.DELETED)) {
            throw new ConflictException("This phone number is already in use.");
        }

        // Cooldown: block a fresh OTP within the resend window of the previous request.
        phoneChangeRepo.findTopByUserIdOrderByCreatedAtDesc(userId).ifPresent(prev -> {
            if (prev.getStatus() == Status.PENDING_VERIFICATION && prev.getCreatedAt() != null) {
                long secs = ChronoUnit.SECONDS.between(prev.getCreatedAt(), LocalDateTime.now());
                if (secs < OTP_RESEND_SEC) {
                    throw new TooManyRequestsException(
                            "Please wait before requesting another verification code.");
                }
            }
        });

        // Self-service: clear this user's prior unverified requests so they can always restart.
        phoneChangeRepo.deleteByUserIdAndStatusIn(userId, UNRESOLVED_STATUSES);

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        PhoneChangeRequestEntity entity = PhoneChangeRequestEntity.builder()
                .userId(userId)
                .currentPhone(user.getPhone())
                .newPhone(newPhone)
                .status(Status.PENDING_VERIFICATION)
                .otpHash(HashUtil.sha256Hex(otp))
                .expiresAt(LocalDateTime.now().plusSeconds(OTP_EXPIRY_SEC))
                .build();
        phoneChangeRepo.save(entity);

        // Deliver the OTP over email (existing OTP channel) to the account's registered address.
        mailService.sendPhoneChangeVerificationOtp(user.getEmail(), otp, OTP_EXPIRY_SEC / 60);
        log.info("PhoneChangeService.createRequest() - OTP emailed for newPhone={}", LogMaskUtil.maskPhone(newPhone));

        MessageResponse response = new MessageResponse();
        response.setMessage("A verification code has been sent to your registered email address.");
        return response;
    }

    @Override
    @Transactional
    public PhoneChangeRequestDto verifyOtp(Long userId, PhoneChangeVerifyRequest request) {
        log.info("PhoneChangeService.verifyOtp() userId={}", userId);

        PhoneChangeRequestEntity entity = phoneChangeRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("PhoneChangeRequest", "userId", userId));

        if (entity.getStatus() != Status.PENDING_VERIFICATION) {
            throw new BadRequestException("No pending verification for a phone change request.");
        }

        if (entity.isVerifyUsed() || entity.getExpiresAt() == null
                || entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Verification code has expired. Please start the request again.");
        }

        if (entity.getAttempts() >= OTP_MAX_ATTEMPTS) {
            entity.setVerifyUsed(true);
            entity.setStatus(Status.REJECTED);
            entity.setReason("Too many incorrect attempts.");
            entity.setDecidedAt(LocalDateTime.now());
            phoneChangeRepo.save(entity);
            throw new UnauthorizedException("Too many incorrect attempts. Please start the request again.");
        }

        entity.setAttempts(entity.getAttempts() + 1);

        if (!HashUtil.sha256Hex(request.getOtp()).equals(entity.getOtpHash())) {
            phoneChangeRepo.save(entity);
            int remaining = OTP_MAX_ATTEMPTS - entity.getAttempts();
            throw new UnauthorizedException(remaining > 0
                    ? "Incorrect code. " + remaining + " attempt(s) remaining."
                    : "Incorrect code. No attempts remaining — please start the request again.");
        }

        entity.setVerifyUsed(true);

        // Ownership proven via OTP — apply immediately. Re-check uniqueness in case the number was
        // claimed by someone else between request and verification.
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (userRepository.isPhoneInUseByLiveAccount(entity.getNewPhone(), UserEntity.AccountStatus.DELETED)) {
            entity.setStatus(Status.REJECTED);
            entity.setReason("This phone number is already in use.");
            entity.setDecidedAt(LocalDateTime.now());
            phoneChangeRepo.save(entity);
            throw new ConflictException("This phone number is already in use.");
        }

        String oldPhone = user.getPhone();
        user.setPhone(entity.getNewPhone());
        user.setActivePhone(entity.getNewPhone()); // keep login/uniqueness column in sync
        user.setPhoneVerified(true);
        // tokenVersion is intentionally NOT bumped: phone is an identifier, not a credential, and
        // the JWT is bound to userId — keep the user signed in (mirrors email change).
        userRepository.save(user);

        entity.setStatus(Status.APPROVED);
        entity.setDecidedAt(LocalDateTime.now());
        phoneChangeRepo.save(entity);

        log.info("PhoneChangeService.verifyOtp() - phone changed userId={} from={} to={} (self-service)",
                user.getId(), LogMaskUtil.maskPhone(oldPhone), LogMaskUtil.maskPhone(entity.getNewPhone()));
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PhoneChangeRequestDto getStatus(Long userId) {
        return phoneChangeRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toDto)
                .orElse(null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Trim + strip spaces/dashes so comparisons and uniqueness are consistent. */
    private String normalize(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[\\s-]", "").trim();
    }

    private PhoneChangeRequestDto toDto(PhoneChangeRequestEntity e) {
        return PhoneChangeRequestDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .currentPhone(e.getCurrentPhone())
                .newPhone(e.getNewPhone())
                .status(e.getStatus().name())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
                .decidedAt(e.getDecidedAt() != null ? e.getDecidedAt().atOffset(ZoneOffset.UTC) : null)
                .reason(e.getReason())
                .build();
    }
}
