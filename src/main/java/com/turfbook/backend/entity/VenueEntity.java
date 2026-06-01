package com.turfbook.backend.entity;

import com.turfbook.backend.entity.converter.JsonListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueEntity {

    public enum VenueStatus {
        PENDING, LIVE, REJECTED, SUSPENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VenueStatus status = VenueStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private double rating = 0.0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "price_per_slot", nullable = false)
    @Builder.Default
    private int pricePerSlot = 0;

    @Column(name = "cover_photo", length = 500)
    private String coverPhoto;

    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private double lat = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private double lng = 0.0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "venue_sports",
            joinColumns = @JoinColumn(name = "venue_id"),
            inverseJoinColumns = @JoinColumn(name = "sport_id")
    )
    @Builder.Default
    private Set<SportEntity> sports = new HashSet<>();

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CourtEntity> courts = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
