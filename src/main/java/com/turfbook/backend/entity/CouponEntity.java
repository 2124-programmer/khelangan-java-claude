package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponEntity {

    public enum DiscountType {
        PERCENT, FLAT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private int discountValue;

    @Column(name = "min_booking", nullable = false)
    @Builder.Default
    private int minBooking = 0;

    @Column(name = "max_discount")
    private Integer maxDiscount;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private int usedCount = 0;

    @Column(name = "max_uses", nullable = false)
    @Builder.Default
    private int maxUses = 100;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
