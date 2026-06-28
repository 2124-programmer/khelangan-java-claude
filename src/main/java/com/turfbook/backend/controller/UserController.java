package com.turfbook.backend.controller;

import com.turfbook.backend.api.UsersApi;
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
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

    private final UserService userService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getMe() {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.getMe() called - userId={}", principal.getId());
        return ResponseEntity.ok(userService.getMe(principal.getId()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateMe(UpdateProfileRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.updateMe() called - userId={}", principal.getId());
        return ResponseEntity.ok(userService.updateMe(principal.getId(), request));
    }

    @PatchMapping("/api/v1/users/me/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> changeMyRole(@Valid @RequestBody ChangeRoleRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.changeMyRole() called - userId={}", principal.getId());
        return ResponseEntity.ok(userService.changeRole(principal.getId(), request));
    }

    @DeleteMapping("/api/v1/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> deleteMe(@Valid @RequestBody DeleteAccountRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.deleteMe() called - userId={}", principal.getId());
        return ResponseEntity.ok(userService.deleteMe(principal.getId(), request));
    }

    /** List all admins with their effective sub-role (SUPER_ADMIN only — enforced in the service). */
    @GetMapping("/api/v1/admin/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<AdminSummaryDto>> listAdmins() {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.listAdmins() called - actorId={}", principal.getId());
        return ResponseEntity.ok(userService.listAdmins(principal.getId()));
    }

    /** Assign an admin sub-role (SUPER_ADMIN only — enforced in the service). */
    @PatchMapping("/api/v1/admin/users/{id}/admin-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> setAdminRole(
            @PathVariable Long id, @Valid @RequestBody SetAdminRoleRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.setAdminRole() called - actorId={}, targetId={}", principal.getId(), id);
        return ResponseEntity.ok(userService.setAdminRole(principal.getId(), id, request));
    }

    /** Create a brand-new admin account (SUPER_ADMIN only — enforced in the service). */
    @PostMapping("/api/v1/admin/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminSummaryDto> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.createAdmin() called - actorId={}", principal.getId());
        return ResponseEntity.ok(userService.createAdmin(principal.getId(), request));
    }

    /** Promote an existing user to admin by email (SUPER_ADMIN only — enforced in the service). */
    @PostMapping("/api/v1/admin/admins/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminSummaryDto> promoteToAdmin(@Valid @RequestBody PromoteAdminRequest request) {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.promoteToAdmin() called - actorId={}", principal.getId());
        return ResponseEntity.ok(userService.promoteToAdmin(principal.getId(), request));
    }

    /**
     * Remove an admin (SUPER_ADMIN only — enforced in the service).
     * {@code mode=demote} reverts to a player; {@code mode=deactivate} soft-deletes the account.
     */
    @DeleteMapping("/api/v1/admin/admins/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> removeAdmin(
            @PathVariable Long id,
            @RequestParam(name = "mode", defaultValue = "demote") String mode) {
        UserPrincipal principal = getPrincipal();
        log.info("UserController.removeAdmin() called - actorId={}, targetId={}, mode={}", principal.getId(), id, mode);
        return ResponseEntity.ok(userService.removeAdmin(principal.getId(), id, mode));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
