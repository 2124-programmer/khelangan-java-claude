package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEntity {

    /** Case lifecycle. OPEN/RESOLVED are the legacy values; the rest power the admin triage flow. */
    public enum DisputeStatus {
        OPEN, UNDER_REVIEW, NEEDS_INFO, RESOLVED, DISMISSED
    }

    public enum Category {
        OWNER_NO_SHOW, OWNER_CANCELLATION, DOUBLE_BOOKING, NOT_AS_DESCRIBED,
        REFUND_NOT_GIVEN, OVERCHARGED, SAFETY_BEHAVIOR, OTHER
    }

    public enum Priority { LOW, MEDIUM, HIGH }

    public enum PartyRole { PLAYER, OWNER }

    public enum WaitingOn { PLAYER, OWNER, NONE }

    public enum FaultParty { PLAYER, OWNER, NONE }

    public enum Outcome { RULED_FOR_PLAYER, RULED_FOR_OWNER, PARTIAL, RESOLVED_BY_PARTIES, DISMISSED }

    public enum Consequence { NONE, WARN, FLAG, SUSPEND, BAN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingEntity booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private UserEntity player;

    @Column(name = "player_name", nullable = false, length = 100)
    private String playerName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(name = "venue_name", nullable = false, length = 200)
    private String venueName;

    /** Original complaint text (legacy). Mirrored as the first conversation message on create. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    // ── Triage metadata ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    @Builder.Default
    private Category category = Category.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "raised_by_role", length = 10)
    @Builder.Default
    private PartyRole raisedByRole = PartyRole.PLAYER;

    @Column(name = "raised_at")
    private LocalDateTime raisedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private UserEntity assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "waiting_on", length = 10)
    @Builder.Default
    private WaitingOn waitingOn = WaitingOn.NONE;

    @Column(name = "sla_hours", nullable = false)
    @Builder.Default
    private int slaHours = 48;

    // ── Resolution (no money is ever processed) ─────────────────────────────
    @Column(name = "resolved_note", columnDefinition = "TEXT")
    private String resolvedNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private Outcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "at_fault", length = 10)
    private FaultParty atFault;

    @Column(name = "ruling_note", columnDefinition = "TEXT")
    private String rulingNote;

    /** Informational ₹ only — the platform never debits/credits; owner is asked to settle offline. */
    @Column(name = "recommended_refund_amount")
    private Integer recommendedRefundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "consequence_target", length = 10)
    private FaultParty consequenceTarget;

    @Enumerated(EnumType.STRING)
    @Column(name = "consequence_action", length = 12)
    private Consequence consequenceAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private UserEntity resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDate createdAt = LocalDate.now();
}
