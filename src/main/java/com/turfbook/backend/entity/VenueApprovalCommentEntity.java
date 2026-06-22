package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One entry in a venue's approval thread. Records each round-trip between the admin and the owner
 * (submit, changes requested, reject, resubmit, approve) so both sides can track the history.
 */
@Entity
@Table(name = "venue_approval_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueApprovalCommentEntity {

    public enum Action {
        SUBMITTED, CHANGES_REQUESTED, REJECTED, RESUBMITTED, APPROVED
    }

    public enum AuthorRole {
        ADMIN, OWNER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 10)
    private AuthorRole authorRole;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
