package com.turfbook.backend.repository;

import com.turfbook.backend.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHashAndUsedFalseAndExpiresAtAfter(
        String tokenHash, LocalDateTime now
    );

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
