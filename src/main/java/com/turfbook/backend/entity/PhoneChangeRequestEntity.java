package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Phone-change verification request — the SMS-OTP analogue of {@link EmailChangeRequestEntity}.
 * Self-contained: it carries its own OTP hash, expiry, attempt counter and cooldown timestamp
 * (no shared otp_records row, since those are email-keyed). Verifying the OTP applies the change
 * immediately (self-service); there is no admin review.
 */
@Entity
@Table(
    name = "phone_change_requests",
    indexes = {
        @Index(name = "idx_pcr_user", columnList = "user_id"),
        @Index(name = "idx_pcr_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneChangeRequestEntity {

    public enum Status {
        PENDING_VERIFICATION,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "current_phone", length = 20)
    private String currentPhone;

    @Column(name = "new_phone", nullable = false, length = 20)
    private String newPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.PENDING_VERIFICATION;

    /** SHA-256 hex of the OTP sent to the new phone for ownership verification. */
    @Column(name = "otp_hash", length = 64)
    private String otpHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "verify_used", nullable = false)
    @Builder.Default
    private boolean verifyUsed = false;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
