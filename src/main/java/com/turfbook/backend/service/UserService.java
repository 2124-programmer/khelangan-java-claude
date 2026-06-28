package com.turfbook.backend.service;

import com.turfbook.backend.dto.AdminSummaryDto;
import com.turfbook.backend.dto.AuthResponse;
import com.turfbook.backend.dto.ChangeRoleRequest;
import com.turfbook.backend.dto.CreateAdminRequest;
import com.turfbook.backend.dto.DeleteAccountRequest;
import com.turfbook.backend.dto.MessageResponse;
import com.turfbook.backend.dto.PromoteAdminRequest;
import com.turfbook.backend.dto.SetAdminRoleRequest;
import com.turfbook.backend.dto.UpdateProfileRequest;
import com.turfbook.backend.dto.UserDto;
import com.turfbook.backend.dto.UserPage;
import com.turfbook.backend.entity.UserEntity;

public interface UserService {

    UserDto getMe(Long userId);

    UserDto updateMe(Long userId, UpdateProfileRequest request);

    /**
     * Self-service account closure (soft-delete) after password re-authentication. Cancels the
     * caller's upcoming bookings, frees their active_email/active_phone for reuse, and force-logs
     * out all sessions. Terminal — the row is retained for history but the account is closed.
     */
    MessageResponse deleteMe(Long userId, DeleteAccountRequest request);

    /**
     * Changes the caller's role (PLAYER ↔ OWNER) after password re-authentication.
     * Returns a fresh AuthResponse so the client can replace the stale JWT immediately.
     */
    AuthResponse changeRole(Long userId, ChangeRoleRequest request);

    UserPage listUsers(int page, int size, String role, String search);

    UserDto blockUser(Long id);

    UserDto unblockUser(Long id);

    /** List all admin accounts with their effective sub-role (SUPER_ADMIN only). */
    java.util.List<AdminSummaryDto> listAdmins(Long actorId);

    /** Assign an admin sub-role to an ADMIN user (SUPER_ADMIN only). */
    MessageResponse setAdminRole(Long actorId, Long targetUserId, SetAdminRoleRequest request);

    /** Create a brand-new ADMIN account with the given sub-role (SUPER_ADMIN only). */
    AdminSummaryDto createAdmin(Long actorId, CreateAdminRequest request);

    /** Promote an existing non-admin user to ADMIN by email (SUPER_ADMIN only). */
    AdminSummaryDto promoteToAdmin(Long actorId, PromoteAdminRequest request);

    /**
     * Remove an admin (SUPER_ADMIN only). {@code mode}:
     * "demote" reverts the account to a PLAYER (keeps login); "deactivate" soft-deletes it.
     */
    MessageResponse removeAdmin(Long actorId, Long targetUserId, String mode);

    UserEntity getEntityById(Long id);
}
