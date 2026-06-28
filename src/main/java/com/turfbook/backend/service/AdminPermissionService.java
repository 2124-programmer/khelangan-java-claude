package com.turfbook.backend.service;

import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Central RBAC for admin sub-roles (SUPER_ADMIN / SUPPORT / READ_ONLY).
 *
 * <ul>
 *   <li>READ_ONLY  — may view everything, may not mutate anything.</li>
 *   <li>SUPPORT    — soft moderation (suspend, verify, message, notes, dispute handling) but NOT ban/delete.</li>
 *   <li>SUPER_ADMIN— everything, including ban and delete.</li>
 * </ul>
 *
 * A legacy admin whose {@code adminRole} is NULL is treated as SUPER_ADMIN, so existing accounts keep
 * full access without a data migration.
 */
@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private final UserRepository userRepository;

    /** Action codes only a SUPER_ADMIN may perform; SUPPORT sees them stripped from availableActions. */
    private static final Set<String> HARD_ACTIONS = Set.of("BAN", "UNBAN", "DELETE");

    /** Resolve an admin's effective sub-role; null for non-admins. Legacy NULL ⇒ SUPER_ADMIN. */
    @Transactional(readOnly = true)
    public UserEntity.AdminRole roleOf(Long actorId) {
        if (actorId == null) return null;
        UserEntity actor = userRepository.findById(actorId).orElse(null);
        if (actor == null || actor.getRole() != UserEntity.Role.ADMIN) return null;
        return actor.getAdminRole() != null ? actor.getAdminRole() : UserEntity.AdminRole.SUPER_ADMIN;
    }

    public boolean canWrite(Long actorId) {
        UserEntity.AdminRole r = roleOf(actorId);
        return r == UserEntity.AdminRole.SUPER_ADMIN || r == UserEntity.AdminRole.SUPPORT;
    }

    public boolean canModerateHard(Long actorId) {
        return roleOf(actorId) == UserEntity.AdminRole.SUPER_ADMIN;
    }

    /** Block READ_ONLY admins from any mutating action. */
    public void requireWrite(Long actorId) {
        if (!canWrite(actorId)) {
            throw new ForbiddenException("Your admin role is read-only and cannot perform this action.");
        }
    }

    /** Gate ban/delete (and admin-role assignment) to SUPER_ADMIN only. */
    public void requireModerateHard(Long actorId) {
        if (!canModerateHard(actorId)) {
            throw new ForbiddenException("Only a super-admin can ban, delete, or change admin roles.");
        }
    }

    /** Gate super-admin-only configuration (platform settings, app config) to SUPER_ADMIN only. */
    public void requireSuperAdmin(Long actorId) {
        if (!canModerateHard(actorId)) {
            throw new ForbiddenException("Only a super-admin can manage platform settings.");
        }
    }

    /**
     * Role-filter a server-computed {@code availableActions} list for the CURRENT caller:
     * READ_ONLY → none; SUPPORT → minus ban/unban/delete; SUPER_ADMIN → unchanged.
     */
    public List<String> filterActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) return actions;
        UserEntity.AdminRole r = roleOf(currentActorId());
        if (r == UserEntity.AdminRole.READ_ONLY) return List.of();
        if (r == UserEntity.AdminRole.SUPPORT) {
            return actions.stream().filter(a -> !HARD_ACTIONS.contains(a)).toList();
        }
        return actions; // SUPER_ADMIN (or unknown → unchanged)
    }

    /** The authenticated admin's id from the security context, or null if unavailable. */
    public Long currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        return null;
    }
}
