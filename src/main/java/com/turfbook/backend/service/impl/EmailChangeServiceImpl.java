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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    private static final List<Status> ACTIVE_STATUSES = List.of(
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

        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("This email address is already in use.");
        }

        if (emailChangeRepo.existsByNewEmailAndStatusIn(newEmail, ACTIVE_STATUSES)) {
            throw new ConflictException("Another account already has a pending request for this email.");
        }

        if (emailChangeRepo.existsByUserIdAndStatusIn(userId, ACTIVE_STATUSES)) {
            throw new ConflictException(
                    "You already have a pending email change request. Please wait for it to be processed.");
        }

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
        entity.setStatus(Status.PENDING);
        emailChangeRepo.save(entity);

        log.info("EmailChangeService.verifyOtp() - request {} moved to PENDING", entity.getId());
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailChangeRequestDto getStatus(Long userId) {
        return emailChangeRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailChangeRequestDto> adminList(String statusStr, int page, int size) {
        Status status = Status.PENDING;
        if (statusStr != null) {
            try {
                status = Status.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return emailChangeRepo.findByStatusOrderByCreatedAtAsc(status, pageable).map(this::toDto);
    }

    @Override
    @Transactional
    public EmailChangeRequestDto adminApprove(Long requestId) {
        log.info("EmailChangeService.adminApprove() requestId={}", requestId);
        EmailChangeRequestEntity entity = emailChangeRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("EmailChangeRequest", "id", requestId));

        if (entity.getStatus() != Status.PENDING) {
            throw new BadRequestException("Only PENDING requests can be approved.");
        }

        UserEntity user = userRepository.findById(entity.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", entity.getUserId()));

        String oldEmail = user.getEmail();
        user.setEmail(entity.getNewEmail());
        user.setTokenVersion(user.getTokenVersion() + 1); // invalidate all existing sessions
        userRepository.save(user);

        entity.setStatus(Status.APPROVED);
        entity.setDecidedAt(LocalDateTime.now());
        emailChangeRepo.save(entity);

        mailService.sendEmailChangeApproved(entity.getNewEmail(), entity.getNewEmail());
        log.info("EmailChangeService.adminApprove() - email changed userId={} from={} to={}",
                user.getId(), LogMaskUtil.maskEmail(oldEmail), LogMaskUtil.maskEmail(entity.getNewEmail()));
        return toDto(entity);
    }

    @Override
    @Transactional
    public EmailChangeRequestDto adminReject(Long requestId, EmailChangeRejectRequest request) {
        log.info("EmailChangeService.adminReject() requestId={}", requestId);
        EmailChangeRequestEntity entity = emailChangeRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("EmailChangeRequest", "id", requestId));

        if (entity.getStatus() != Status.PENDING) {
            throw new BadRequestException("Only PENDING requests can be rejected.");
        }

        entity.setStatus(Status.REJECTED);
        entity.setDecidedAt(LocalDateTime.now());
        entity.setReason(request.getReason());
        emailChangeRepo.save(entity);

        UserEntity user = userRepository.findById(entity.getUserId()).orElse(null);
        if (user != null) {
            mailService.sendEmailChangeRejected(user.getEmail(), entity.getNewEmail(), request.getReason());
        }
        return toDto(entity);
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
