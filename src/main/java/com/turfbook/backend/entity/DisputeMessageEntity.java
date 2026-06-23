package com.turfbook.backend.entity;

import com.turfbook.backend.entity.converter.JsonListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One entry in the PARTY-VISIBLE conversation of a dispute (complaint, admin↔party messages,
 * party replies). Kept in a separate table from {@link DisputeNoteEntity} so internal notes can
 * never be serialized into the party-facing payload.
 */
@Entity
@Table(name = "dispute_messages", indexes = @Index(name = "idx_dispute_msg", columnList = "dispute_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispute_id", nullable = false)
    private DisputeEntity dispute;

    /** PLAYER, OWNER or ADMIN. */
    @Column(name = "sender_role", nullable = false, length = 10)
    private String senderRole;

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** View-only image URLs uploaded by parties as evidence. */
    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
