package com.turfbook.backend.repository;

import com.turfbook.backend.entity.OtpRecordEntity;
import com.turfbook.backend.entity.OtpRecordEntity.Purpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecordEntity, Long> {

    // ── Existing login-OTP queries (purpose = AUTH_OTP) ──────────────────────

    Optional<OtpRecordEntity> findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
        String email, LocalDateTime now
    );

    Optional<OtpRecordEntity> findTopByEmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("DELETE FROM OtpRecordEntity o WHERE o.email = :email")
    void deleteAllByEmail(String email);

    // ── Purpose-scoped queries ────────────────────────────────────────────────

    Optional<OtpRecordEntity> findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
        String email, Purpose purpose, LocalDateTime now
    );

    Optional<OtpRecordEntity> findTopByEmailAndPurposeOrderByCreatedAtDesc(
        String email, Purpose purpose
    );

    @Modifying
    @Query("DELETE FROM OtpRecordEntity o WHERE o.email = :email AND o.purpose = :purpose")
    void deleteAllByEmailAndPurpose(@Param("email") String email, @Param("purpose") Purpose purpose);

    @Query("SELECT COUNT(o) FROM OtpRecordEntity o WHERE o.email = :email AND o.purpose = :purpose AND o.createdAt > :since")
    long countByEmailAndPurposeAndCreatedAtAfter(
        @Param("email") String email,
        @Param("purpose") Purpose purpose,
        @Param("since") LocalDateTime since
    );
}
