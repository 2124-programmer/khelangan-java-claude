package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "owner_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private UserEntity owner;

    @Column(name = "auto_accept_bookings", nullable = false)
    @Builder.Default
    private boolean autoAcceptBookings = false;

    @Column(name = "push_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean pushNotificationsEnabled = true;
}
