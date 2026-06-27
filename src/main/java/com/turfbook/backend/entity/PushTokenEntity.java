package com.turfbook.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A device push token (Expo push token) bound to a user. One user may have several (multiple
 * devices); a token is globally unique and is re-pointed to the latest user that registers it.
 */
@Entity
@Table(
    name = "push_tokens",
    uniqueConstraints = @UniqueConstraint(name = "uq_push_token", columnNames = "token"),
    indexes = { @Index(name = "idx_push_user", columnList = "user_id") }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 255)
    private String token;

    /** "ios" | "android" | "web" — informational. */
    @Column(length = 20)
    private String platform;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
