package com.turfbook.backend.service;

import com.turfbook.backend.dto.AuthResponse;
import com.turfbook.backend.dto.ChangeRoleRequest;
import com.turfbook.backend.dto.UpdateProfileRequest;
import com.turfbook.backend.dto.UserDto;
import com.turfbook.backend.dto.UserPage;
import com.turfbook.backend.entity.UserEntity;

public interface UserService {

    UserDto getMe(Long userId);

    UserDto updateMe(Long userId, UpdateProfileRequest request);

    /**
     * Changes the caller's role (PLAYER ↔ OWNER) after password re-authentication.
     * Returns a fresh AuthResponse so the client can replace the stale JWT immediately.
     */
    AuthResponse changeRole(Long userId, ChangeRoleRequest request);

    UserPage listUsers(int page, int size, String role, String search);

    UserDto blockUser(Long id);

    UserDto unblockUser(Long id);

    UserEntity getEntityById(Long id);
}
