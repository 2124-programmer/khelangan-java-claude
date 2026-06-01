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

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "price_per_slot", nullable = false)
    @Builder.Default
    private int pricePerSlot = 0;

    @Column(name = "peak_price", nullable = false)
    @Builder.Default
    private int peakPrice = 0;

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SlotEntity> slots = new ArrayList<>();
}
