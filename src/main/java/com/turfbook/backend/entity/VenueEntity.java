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
        DRAFT, PENDING, LIVE, REJECTED, SUSPENDED, CHANGES_REQUESTED,
        /** Terminal: removed from the marketplace because the owner account was deleted. */
        ARCHIVED
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
    private VenueStatus status = VenueStatus.DRAFT;

    /**
     * Tier the owner committed to at submission for venues over the free court threshold (>2).
     * Null for ≤2-court venues. Approval no longer auto-starts a trial: after approval the owner
     * self-activates a trial or a paid plan and chooses which courts to make bookable.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "intended_plan_code", length = 20)
    private PlanCode intendedPlanCode;

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

    /**
     * Denormalized live-gating flag, recomputed on every subscription transition.
     * True only when the venue holds a current TRIALING/ACTIVE subscription within its
     * period. The player venues query requires status=LIVE AND this flag true.
     */
    @Column(name = "subscription_active", nullable = false)
    @Builder.Default
    private boolean subscriptionActive = false;

    /**
     * Placement boost from the active plan (0 when no PRIORITY_PLACEMENT feature).
     * Used to rank venues ahead of the default rating/distance ordering.
     */
    @Column(name = "placement_weight", nullable = false)
    @Builder.Default
    private int placementWeight = 0;

    /** Denormalized: true when the active plan grants FEATURED_BADGE (player UI badge). */
    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    /**
     * Denormalized count of player-bookable courts (active courts covered by the current
     * live subscription), recomputed on every subscription/court transition. The player feed
     * requires status=LIVE AND subscriptionActive AND this count &gt; 0, so a venue with no
     * covered courts is omitted from discovery entirely.
     */
    @Column(name = "bookable_court_count", nullable = false)
    @Builder.Default
    private int bookableCourtCount = 0;

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

    /** When the venue was first approved (status → LIVE). Null until approved. Drives the lifecycle timeline. */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * True when this venue was unlisted (LIVE → SUSPENDED) specifically because its OWNER was
     * suspended/banned — not by a per-venue admin action. Owner reactivation relists exactly
     * these venues, leaving venues an admin unlisted independently untouched.
     */
    @Column(name = "unlisted_by_owner", nullable = false)
    @Builder.Default
    private boolean unlistedByOwner = false;
}
