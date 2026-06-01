package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEntity {

    public enum DisputeStatus {
        OPEN, RESOLVED
    }

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(name = "resolved_note", columnDefinition = "TEXT")
    private String resolvedNote;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDate createdAt = LocalDate.now();
}
