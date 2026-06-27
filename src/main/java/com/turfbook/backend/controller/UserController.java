package com.turfbook.backend.controller;

import com.turfbook.backend.api.UsersApi;
import com.turfbook.backend.dto.AuthResponse;
import com.turfbook.backend.dto.ChangeRoleRequest;
import com.turfbook.backend.dto.DeleteAccountRequest;
import com.turfbook.backend.dto.MessageResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
