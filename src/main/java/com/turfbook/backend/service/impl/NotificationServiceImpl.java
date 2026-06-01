package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.BroadcastRequest;
import com.turfbook.backend.dto.MessageResponse;
import com.turfbook.backend.dto.NotificationDto;
import com.turfbook.backend.dto.NotificationPage;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.NotificationMapper;
import com.turfbook.backend.repository.NotificationRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public NotificationPage listNotifications(Long userId, int page, int size) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationEntity> entityPage = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        NotificationPage dto = new NotificationPage();
        dto.setContent(entityPage.getContent().stream().map(notificationMapper::toDto).toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }

    @Override
    @Transactional
    public NotificationDto markRead(Long id, Long userId) {
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not own this notification");
        }

        notification.setRead(true);
        return notificationMapper.toDto(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public MessageResponse markAllRead(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        int count = notificationRepository.markAllReadByUser(user);
        MessageResponse response = new MessageResponse();
        response.setMessage(count + " notifications marked as read");
        return response;
    }

    @Override
    @Transactional
    public MessageResponse broadcast(BroadcastRequest request) {
        List<UserEntity> targets;
        String audience = request.getAudience() != null ? request.getAudience().toString() : "ALL";

        switch (audience) {
            case "PLAYERS" -> targets = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserEntity.Role.PLAYER).toList();
            case "OWNERS" -> targets = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserEntity.Role.OWNER).toList();
            default -> targets = userRepository.findAll();
        }

        for (UserEntity user : targets) {
            createNotification(user, request.getTitle(), request.getBody(),
                    NotificationEntity.NotificationType.SYSTEM);
        }

        MessageResponse response = new MessageResponse();
        response.setMessage("Broadcast sent to " + targets.size() + " users");
        return response;
    }

    @Override
    @Transactional
    public void createNotification(UserEntity user, String title, String body,
                                    NotificationEntity.NotificationType type) {
        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
        log.debug("Notification created for user {}: {}", user.getId(), title);
    }
}
