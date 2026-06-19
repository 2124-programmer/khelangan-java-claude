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

    /** Street address / line 1. Stored in the legacy "address" column. */
    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "contact_phone", length = 15)
    private String contactPhone;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /** First bookable hour, "HH:00" format (e.g. "05:00"). */
    @Column(name = "open_time", nullable = false, length = 5)
    @Builder.Default
    private String openTime = "05:00";

    /** Last slot ends at this hour, "HH:00" format (e.g. "23:00"). */
    @Column(name = "close_time", nullable = false, length = 5)
    @Builder.Default
    private String closeTime = "23:00";

    /** Venue-level default price per hour (₹, integer ≥ 0). */
    @Column(name = "price_per_hour", nullable = false)
    @Builder.Default
    private int pricePerHour = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VenueStatus status = VenueStatus.PENDING;

    @Column(name = "rating")
    private Double ratingAverage = null;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private long ratingCount = 0L;

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

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

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
