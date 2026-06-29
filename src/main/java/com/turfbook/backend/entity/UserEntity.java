package com.turfbook.backend.entity;

import com.turfbook.backend.entity.converter.JsonListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    public enum Role {
        PLAYER, OWNER, ADMIN
    }

    /**
     * Admin sub-role (only meaningful when {@link #role} == ADMIN). Governs what an admin may do:
     * SUPER_ADMIN → everything (incl. ban/delete); SUPPORT → soft moderation, no ban/delete;
     * READ_ONLY → view only, no mutations. NULL on a legacy admin is treated as SUPER_ADMIN so
     * existing admins keep full access without a migration.
     */
    public enum AdminRole {
        SUPER_ADMIN, SUPPORT, READ_ONLY
    }

    /**
     * Account standing for admin moderation.
     * ACTIVE → normal; SUSPENDED → temporary cool-down; BANNED → identifiers retained + locked;
     * DELETED → closed (identifier release lands in Phase 2). {@link #isBlocked} stays in sync so
     * the existing login/OTP block-check denies access without touching the auth core.
     */
    public enum AccountStatus {
        ACTIVE, SUSPENDED, BANNED, DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Original sign-up email — kept for history and never nulled. Uniqueness moved to
     * {@link #activeEmail}; this column is intentionally NOT unique so a DELETED row can
     * retain its address while a fresh sign-up reclaims it via {@code active_email}.
     */
    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    /**
     * The address login/registration uniqueness is enforced against (nullable-UNIQUE).
     * Equals {@link #email} for live accounts; NULL once the account is soft-DELETED so the
     * email frees up for reuse. MySQL allows many NULLs, so deleted rows coexist with a new
     * sign-up that claims the value. The unique index is created by the migration runner.
     */
    @Column(name = "active_email", length = 255)
    private String activeEmail;

    /** Phone counterpart of {@link #activeEmail} (nullable-UNIQUE). NULLed on soft-delete. */
    @Column(name = "active_phone", length = 20)
    private String activePhone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Admin sub-role; NULL for non-admins and legacy admins (legacy NULL ⇒ treated as SUPER_ADMIN). */
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_role", length = 20)
    private AdminRole adminRole;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "preferred_sports", columnDefinition = "json")
    @Builder.Default
    private List<String> preferredSports = new ArrayList<>();

    @Column(name = "total_bookings", nullable = false)
    @Builder.Default
    private int totalBookings = 0;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private boolean isBlocked = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    /** When the user accepted the Terms & Privacy Policy at sign-up (DPDP consent record). */
    @Column(name = "accepted_terms_at")
    private LocalDateTime acceptedTermsAt;

    @Column(name = "suspended_reason", length = 500)
    private String suspendedReason;

    @Column(name = "suspended_until")
    private LocalDate suspendedUntil;

    /** Stamped on moderation transitions (and could later track real engagement). */
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    /**
     * Number of disputes this party has been found at fault in (or FLAGged via a dispute ruling).
     * Feeds the Players/Owners risk model so repeat offenders surface automatically.
     */
    @Column(name = "dispute_at_fault_count", nullable = false)
    @Builder.Default
    private int disputeAtFaultCount = 0;

    /** Incremented on password change/reset/email-change and admin force-logout to invalidate all JWTs. */
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private int tokenVersion = 0;

    /**
     * When true, the user must change their password before accessing any other endpoint.
     * Set on bootstrap-seeded super-admins (see AdminSeeder); cleared on the next successful
     * change-password. Enforced server-side in JwtAuthenticationFilter. (ddl-auto adds this column.)
     */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    // ── Soft-delete (terminal) ─────────────────────────────────────────────
    /** When the account was soft-deleted. Non-null ⇒ DELETED + read-only everywhere. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Admin who performed the deletion (actor id). */
    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "deleted_reason", length = 500)
    private String deletedReason;

    /** Cascade tallies captured at delete time, surfaced on the deletion banner. */
    @Column(name = "deleted_venues_archived")
    private Integer deletedVenuesArchived;

    @Column(name = "deleted_bookings_cancelled")
    private Integer deletedBookingsCancelled;

    @Column(name = "deleted_subscriptions_voided")
    private Integer deletedSubscriptionsVoided;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
