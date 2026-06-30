package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * An owner-initiated request to change a venue's LIVE court set after activation. Owners may freely
 * activate a DRAFT court into a FREE subscription slot, but once a court is LIVE (holds a slot) only
 * a SUPER_ADMIN may free or swap it — the owner files this request and a super-admin approves/rejects.
 *
 * <p>Shape: free {@code liveCourtId} (the currently-live court to take out of the live set) and,
 * optionally, put {@code draftCourtId} live in its place (a swap). Approval is re-validated against
 * current state so a request that was overtaken by another change is rejected as stale.
 */
@Entity
@Table(name = "court_change_requests", indexes = {
        @Index(name = "idx_ccr_venue", columnList = "venue_id"),
        @Index(name = "idx_ccr_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtChangeRequestEntity {

    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    /** The currently-LIVE court the owner wants taken out of the live set (freed). */
    @Column(name = "live_court_id", nullable = false)
    private Long liveCourtId;

    /** Optional: a DRAFT court to put LIVE in the freed slot (a swap). Null = free only. */
    @Column(name = "draft_court_id")
    private Long draftCourtId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String reason;

    /** Super-admin who approved/rejected (null while pending). */
    @Column(name = "decided_by")
    private Long decidedBy;

    /** Note from the super-admin on the decision, or the stale/auto reason. */
    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
