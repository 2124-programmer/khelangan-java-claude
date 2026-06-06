package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEntity {

    public enum BookingStatus {
        PENDING, CONFIRMED, COMPLETED, CANCELLED, REJECTED, EXPIRED
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, REFUNDED
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private CourtEntity court;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "slot_id", nullable = false)
    private SlotEntity slot;

    @Column(nullable = false, length = 100)
    private String sport;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private int amount = 0;

    @Column(name = "convenience_fee", nullable = false)
    @Builder.Default
    private int convenienceFee = 20;

    @Column(nullable = false)
    @Builder.Default
    private int discount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int commission = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "has_review", nullable = false)
    @Builder.Default
    private boolean hasReview = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
