package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row per time a player taps Call/WhatsApp on a venue's "Contact venue" sheet.
 * Every intent is recorded (analytics / "who contacted me"); the {@code notified}
 * flag marks the rows that actually fired an owner notification, so repeated taps
 * within the cooldown window can be deduped without losing the intent history.
 */
@Entity
@Table(name = "venue_contact_intents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactIntentEntity {

    public enum Channel {
        CALL, WHATSAPP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private UserEntity player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private Channel channel;

    /** True when this intent triggered an owner notification (vs. deduped within cooldown). */
    @Column(name = "notified", nullable = false)
    @Builder.Default
    private boolean notified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
