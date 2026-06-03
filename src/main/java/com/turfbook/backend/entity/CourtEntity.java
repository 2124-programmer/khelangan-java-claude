package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id", nullable = false)
    private SportEntity sport;

    @Column(length = 50)
    private String type;

    /**
     * Court-level price per hour (₹). NULL means inherit from venue.pricePerHour.
     * The slot engine resolves: court → venue → global default.
     */
    @Column(name = "price_per_hour")
    private Integer pricePerHour;

    /** NULL means inherit venue.openTime ("HH:00" format). */
    @Column(name = "open_time", length = 5)
    private String openTime;

    /** NULL means inherit venue.closeTime ("HH:00" format). */
    @Column(name = "close_time", length = 5)
    private String closeTime;

    /** Slot length in minutes. Fixed at 60 for now; kept for future 30-min support. */
    @Column(name = "slot_duration_mins", nullable = false)
    @Builder.Default
    private int slotDurationMins = 60;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "peak_price", nullable = false)
    @Builder.Default
    private int peakPrice = 0;

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SlotEntity> slots = new ArrayList<>();

    /** Resolves the effective price per hour, falling back to venue price. */
    public int effectivePricePerHour() {
        return pricePerHour != null ? pricePerHour : venue.getPricePerHour();
    }

    /** Resolves the effective open time, falling back to venue open time. */
    public String effectiveOpenTime() {
        return openTime != null ? openTime : venue.getOpenTime();
    }

    /** Resolves the effective close time, falling back to venue close time. */
    public String effectiveCloseTime() {
        return closeTime != null ? closeTime : venue.getCloseTime();
    }
}
