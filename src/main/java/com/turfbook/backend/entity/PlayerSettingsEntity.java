package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Per-player preferences (notification toggles). The player-side counterpart of
 * {@link OwnerSettingsEntity}. {@code pushNotificationsEnabled} is honoured at push-send time.
 */
@Entity
@Table(name = "player_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private UserEntity player;

    @Column(name = "push_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean pushNotificationsEnabled = true;

    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean emailNotificationsEnabled = true;
}
