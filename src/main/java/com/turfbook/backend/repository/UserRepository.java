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

    /**
     * Authoritative "is this email claimed by a live (non-deleted) account?" check. Matches on the
     * raw {@code email} column — which is ALWAYS populated (lower/trimmed at registration) — so it
     * blocks a duplicate even for a legacy/un-backfilled row whose {@code active_email} is NULL.
     * Soft-deleted accounts are excluded, preserving the "deleted frees its email for reuse" rule.
     * This is the guard against two live users sharing an email; {@code active_email} + its unique
     * index remain the login/index backstop.
     */
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u "
            + "WHERE LOWER(TRIM(u.email)) = :email AND u.status <> :excludeStatus")
    boolean isEmailInUseByLiveAccount(@Param("email") String email,
                                      @Param("excludeStatus") UserEntity.AccountStatus excludeStatus);

    /**
     * Phone analogue of {@link #isEmailInUseByLiveAccount}. Normalizes the stored {@code phone}
     * in-query (strip spaces/dashes) so it matches regardless of how the number was originally
     * formatted, and excludes soft-deleted accounts. Robust even when {@code active_phone} is NULL.
     */
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u "
            + "WHERE REPLACE(REPLACE(TRIM(u.phone), ' ', ''), '-', '') = :phone "
            + "AND u.status <> :excludeStatus")
    boolean isPhoneInUseByLiveAccount(@Param("phone") String normalizedPhone,
                                      @Param("excludeStatus") UserEntity.AccountStatus excludeStatus);

    long countByRole(UserEntity.Role role);

    /** Whether any admin of the given sub-role exists — used by the idempotent super-admin bootstrap. */
    boolean existsByRoleAndAdminRole(UserEntity.Role role, UserEntity.AdminRole adminRole);

    /** All users of a role — used to fan a notification out to every admin. */
    java.util.List<UserEntity> findByRole(UserEntity.Role role);

    long countByRoleAndCreatedAtAfter(UserEntity.Role role, LocalDateTime after);

    /** New signups in a window — players + owners created in [from, to). Used by the admin dashboard. */
    long countByRoleInAndCreatedAtBetween(
            java.util.Collection<UserEntity.Role> roles, LocalDateTime from, LocalDateTime to);

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
