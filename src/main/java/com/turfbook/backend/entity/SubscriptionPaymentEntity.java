package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit record for every activation/renewal — including manual cash/UPI offline
 * payments recorded by an admin. Provides a money trail independent of provider.
 */
@Entity
@Table(name = "subscription_payments", indexes = {
        @Index(name = "idx_pay_subscription", columnList = "subscription_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPaymentEntity {

    public enum Method {
        CASH, UPI_OFFLINE, BANK_TRANSFER, PROVIDER
    }

    public enum Status {
        RECORDED, PENDING, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionEntity subscription;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Method method = Method.CASH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.RECORDED;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    /** Admin who recorded an offline payment; null for provider-driven payments. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_admin_id")
    private UserEntity recordedByAdmin;

    @Column(length = 200)
    private String reference;
}
