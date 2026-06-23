package com.turfbook.backend.repository;

import com.turfbook.backend.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    // ── active_* are the uniqueness/login columns (see UserEntity.activeEmail) ──
    /** Resolve a login/OTP/reset by the *active* email. DELETED accounts (active_email = NULL) never match. */
    Optional<UserEntity> findByActiveEmail(String activeEmail);

    boolean existsByActiveEmail(String activeEmail);

    boolean existsByActivePhone(String activePhone);

    long countByRole(UserEntity.Role role);

    /** All users of a role — used to fan a notification out to every admin. */
    java.util.List<UserEntity> findByRole(UserEntity.Role role);

    long countByRoleAndCreatedAtAfter(UserEntity.Role role, LocalDateTime after);

    @Query("SELECT u FROM UserEntity u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UserEntity> findAllByRoleAndSearch(
            @Param("role") UserEntity.Role role,
            @Param("search") String search,
            Pageable pageable
    );
}
