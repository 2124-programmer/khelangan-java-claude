package com.turfbook.backend.repository;

import com.turfbook.backend.entity.OtpRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecordEntity, Long> {

    Optional<OtpRecordEntity> findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
        String email, LocalDateTime now
    );

    Optional<OtpRecordEntity> findTopByEmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("DELETE FROM OtpRecordEntity o WHERE o.email = :email")
    void deleteAllByEmail(String email);
}
