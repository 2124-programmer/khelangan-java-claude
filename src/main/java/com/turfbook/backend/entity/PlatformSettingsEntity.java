package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSettingsEntity {

    @Id
    private Long id;

    @Column(name = "commission_percent", nullable = false)
    @Builder.Default
    private int commissionPercent = 10;

    @Column(name = "convenience_fee", nullable = false)
    @Builder.Default
    private int convenienceFee = 20;

    @Column(name = "maintenance_mode", nullable = false)
    @Builder.Default
    private boolean maintenanceMode = false;

    @Column(name = "auto_approve_venues", nullable = false)
    @Builder.Default
    private boolean autoApproveVenues = false;
}
