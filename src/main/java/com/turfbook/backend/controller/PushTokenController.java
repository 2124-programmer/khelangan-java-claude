package com.turfbook.backend.controller;

import com.turfbook.backend.dto.MessageResponse;
import com.turfbook.backend.dto.RegisterPushTokenRequest;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Device push-token registration. The app registers its Expo push token after permission is
 * granted, and unregisters on logout. Role-neutral (any authenticated user).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PushTokenController {

    private final PushNotificationService pushNotificationService;

    @PostMapping("/api/v1/push-tokens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> register(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterPushTokenRequest request) {
        log.info("PushTokenController.register() userId={}", principal.getId());
        pushNotificationService.registerToken(principal.getId(), request.getToken(), request.getPlatform());
        MessageResponse response = new MessageResponse();
        response.setMessage("Push token registered.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/push-tokens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> unregister(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("token") String token) {
        log.info("PushTokenController.unregister() userId={}", principal.getId());
        pushNotificationService.unregisterToken(principal.getId(), token);
        MessageResponse response = new MessageResponse();
        response.setMessage("Push token removed.");
        return ResponseEntity.ok(response);
    }
}
