package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.EmailChangeRequestEntity;
import com.turfbook.backend.entity.EmailChangeRequestEntity.Status;
import com.turfbook.backend.entity.OtpRecordEntity;
import com.turfbook.backend.entity.OtpRecordEntity.Purpose;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.TooManyRequestsException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.repository.EmailChangeRequestRepository;
import com.turfbook.backend.repository.OtpRecordRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.EmailChangeService;
import com.turfbook.backend.service.MailService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailChangeServiceImpl implements EmailChangeService {

    private final UserRepository userRepository;
    private final EmailChangeRequestRepository emailChangeRepo;
    private final OtpRecordRepository otpRecordRepository;
    private final MailService mailService;

    private static final int OTP_EXPIRY_SEC  = 300;
    private static final int OTP_RESEND_SEC  = 60;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Requests that haven't reached a final state — swept when the user starts a new request. */
    private static final List<Status> UNRESOLVED_STATUSES = List.of(
            Status.PENDING_VERIFICATION, Status.PENDING);

    @Override
    @Transactional
    public MessageResponse createRequest(Long userId, EmailChangeCreateRequest request) {
        String newEmail = request.getNewEmail().toLowerCase().trim();
        log.info("EmailChangeService.createRequest() userId={}, newEmail={}",
                userId, LogMaskUtil.maskEmail(newEmail));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (newEmail.equals(user.getEmail())) {
            throw new BadRequestException("New email is the same as your current email.");
        }

        // Block if ANY live (non-deleted) account already holds this email — matched on the raw
        // column so a legacy row with a NULL active_email still collides (the bug that let two
        // owners share an email). Deleted accounts are excluded, so a freed email can be reclaimed.
        if (userRepository.isEmailInUseByLiveAccount(newEmail, UserEntity.AccountStatus.DELETED)) {
            throw new ConflictException("This email address is already in use.");
        }

        // Self-service: clear this user's prior unverified/legacy requests so they can always
        // restart. An unverified request never reserves an address (real-email uniqueness is
        // enforced above and re-checked at verification), so there's no cross-account block.
        emailChangeRepo.deleteByUserIdAndStatusIn(userId, UNRESOLVED_STATUSES);

        // OTP cooldown check (reuse the EMAIL_CHANGE_VERIFY purpose)
        otpRecordRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(newEmail, Purpose.EMAIL_CHANGE_VERIFY)
                .ifPresent(r -> {
                    long secs = ChronoUnit.SECONDS.between(r.getCreatedAt(), LocalDateTime.now());
                    if (secs < OTP_RESEND_SEC) {
                        throw new TooManyRequestsException(
                                "Please wait before requesting another verification code.");
                    }
                });

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        otpRecordRepository.deleteAllByEmailAndPurpose(newEmail, Purpose.EMAIL_CHANGE_VERIFY);
        OtpRecordEntity otpRecord = OtpRecordEntity.builder()
                .email(newEmail)
                .purpose(Purpose.EMAIL_CHANGE_VERIFY)
                .codeHash(HashUtil.sha256Hex(otp))
                .expiresAt(LocalDateTime.now().plusSeconds(OTP_EXPIRY_SEC))
                .build();
        otpRecordRepository.save(otpRecord);

        EmailChangeRequestEntity entity = EmailChangeRequestEntity.builder()
                .userId(userId)
                .currentEmail(user.getEmail())
                .newEmail(newEmail)
                .status(Status.PENDING_VERIFICATION)
                .verifyOtpHash(HashUtil.sha256Hex(otp))
                .verifyExpiresAt(LocalDateTime.now().plusSeconds(OTP_EXPIRY_SEC))
                .build();
        emailChangeRepo.save(entity);

        mailService.sendEmailChangeVerificationOtp(newEmail, otp, OTP_EXPIRY_SEC / 60);
        log.info("EmailChangeService.createRequest() - OTP sent to newEmail={}", LogMaskUtil.maskEmail(newEmail));

        MessageResponse response = new MessageResponse();
        response.setMessage("A verification code has been sent to your new email address.");
        return response;
    }

    @Override
    @Transactional
    public EmailChangeRequestDto verifyOtp(Long userId, EmailChangeVerifyRequest request) {
        log.info("EmailChangeService.verifyOtp() userId={}", userId);

        EmailChangeRequestEntity entity = emailChangeRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("EmailChangeRequest", "userId", userId));

        if (entity.getStatus() != Status.PENDING_VERIFICATION) {
            throw new BadRequestException("No pending verification for an email change request.");
        }

        if (entity.isVerifyUsed() || entity.getVerifyExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Verification code has expired. Please start the request again.");
        }

        OtpRecordEntity otpRecord = otpRecordRepository
                .findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        entity.getNewEmail(), Purpose.EMAIL_CHANGE_VERIFY, LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException(
                        "Verification code has expired. Please start the request again."));

        if (otpRecord.getAttempts() >= OTP_MAX_ATTEMPTS) {
            otpRecord.setUsed(true);
            otpRecordRepository.save(otpRecord);
            throw new UnauthorizedException("Too many incorrect attempts. Please start the request again.");
        }

        otpRecord.setAttempts(otpRecord.getAttempts() + 1);

        if (!HashUtil.sha256Hex(request.getOtp()).equals(otpRecord.getCodeHash())) {
            otpRecordRepository.save(otpRecord);
            int remaining = OTP_MAX_ATTEMPTS - otpRecord.getAttempts();
            throw new UnauthorizedException(remaining > 0
                    ? "Incorrect code. " + remaining + " attempt(s) remaining."
                    : "Incorrect code. No attempts remaining — please start the request again.");
        }

        otpRecord.setUsed(true);
        otpRecordRepository.save(otpRecord);
        entity.setVerifyUsed(true);

        // Self-service: ownership of the new address is now proven via OTP, so apply the change
        // immediately — no admin review. Re-check uniqueness in case the address was claimed by
        // someone else between the request and this verification.
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (userRepository.isEmailInUseByLiveAccount(entity.getNewEmail(), UserEntity.AccountStatus.DELETED)) {
            entity.setStatus(Status.REJECTED);
            entity.setReason("This email address is already in use.");
            entity.setDecidedAt(LocalDateTime.now());
            emailChangeRepo.save(entity);
            throw new ConflictException("This email address is already in use.");
        }

        String oldEmail = user.getEmail();
        user.setEmail(entity.getNewEmail());
        user.setActiveEmail(entity.getNewEmail()); // keep login/uniqueness column in sync
        // tokenVersion is intentionally NOT bumped: the user proved ownership via OTP and the JWT is
        // bound to userId, so we keep them signed in (no surprise logout). Future logins use the new
        // email. (Password changes still bump tokenVersion — email is an identifier, not a credential.)
        userRepository.save(user);

        entity.setStatus(Status.APPROVED);
        entity.setDecidedAt(LocalDateTime.now());
        emailChangeRepo.save(entity);

        // Confirm to the new address; also alert the old one as a security heads-up.
        mailService.sendEmailChangeApproved(entity.getNewEmail(), entity.getNewEmail());
        mailService.sendEmailChangeApproved(oldEmail, entity.getNewEmail());

        log.info("EmailChangeService.verifyOtp() - email changed userId={} from={} to={} (self-service)",
                user.getId(), LogMaskUtil.maskEmail(oldEmail), LogMaskUtil.maskEmail(entity.getNewEmail()));
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailChangeRequestDto getStatus(Long userId) {
        return emailChangeRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toDto)
                .orElse(null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private EmailChangeRequestDto toDto(EmailChangeRequestEntity e) {
        return EmailChangeRequestDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .currentEmail(e.getCurrentEmail())
                .newEmail(e.getNewEmail())
                .status(e.getStatus().name())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
                .decidedAt(e.getDecidedAt() != null ? e.getDecidedAt().atOffset(ZoneOffset.UTC) : null)
                .reason(e.getReason())
                .build();
    }

}
