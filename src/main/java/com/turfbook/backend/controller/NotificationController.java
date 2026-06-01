package com.turfbook.backend.controller;

import com.turfbook.backend.api.NotificationsApi;
import com.turfbook.backend.dto.MessageResponse;
import com.turfbook.backend.dto.NotificationDto;
import com.turfbook.backend.dto.NotificationPage;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationsApi {

    private final NotificationService notificationService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPage> listNotifications(Integer page, Integer size) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(notificationService.listNotifications(principal.getId(),
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDto> markNotificationRead(Long id) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(notificationService.markRead(id, principal.getId()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> markAllNotificationsRead() {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.ok(notificationService.markAllRead(principal.getId()));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
