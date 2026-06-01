package com.turfbook.backend.service;

import com.turfbook.backend.dto.BroadcastRequest;
import com.turfbook.backend.dto.MessageResponse;
import com.turfbook.backend.dto.NotificationDto;
import com.turfbook.backend.dto.NotificationPage;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.UserEntity;

public interface NotificationService {

    NotificationPage listNotifications(Long userId, int page, int size);

    NotificationDto markRead(Long id, Long userId);

    MessageResponse markAllRead(Long userId);

    MessageResponse broadcast(BroadcastRequest request);

    // Internal helpers for other services
    void createNotification(UserEntity user, String title, String body, NotificationEntity.NotificationType type);
}
