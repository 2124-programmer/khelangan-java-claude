package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "email_change_requests",
    indexes = {
        @Index(name = "idx_ecr_user", columnList = "user_id"),
        @Index(name = "idx_ecr_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeRequestEntity {

    public enum Status {
        PENDING_VERIFICATION,
        PENDING,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "current_email", nullable = false, length = 255)
    private String currentEmail;

    @Column(name = "new_email", nullable = false, length = 255)
    private String newEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.PENDING_VERIFICATION;

    /** SHA-256 hex of the OTP sent to the new email for ownership verification. */
    @Column(name = "verify_otp_hash", length = 64)
    private String verifyOtpHash;

    @Column(name = "verify_expires_at")
    private LocalDateTime verifyExpiresAt;

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
