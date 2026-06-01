package com.turfbook.backend.controller;

import com.turfbook.backend.api.UsersApi;
import com.turfbook.backend.dto.AuthResponse;
import com.turfbook.backend.dto.ChangeRoleRequest;
import com.turfbook.backend.dto.UpdateProfileRequest;
import com.turfbook.backend.dto.UserDto;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

    private final UserService userService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getMe() {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(userService.getMe(principal.getId()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateMe(UpdateProfileRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(userService.updateMe(principal.getId(), request));
    }

    @PatchMapping("/api/v1/users/me/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> changeMyRole(@Valid @RequestBody ChangeRoleRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(userService.changeRole(principal.getId(), request));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
