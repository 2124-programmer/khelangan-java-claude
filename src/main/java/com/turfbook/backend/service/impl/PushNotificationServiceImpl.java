package com.turfbook.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfbook.backend.entity.PushTokenEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.repository.OwnerSettingsRepository;
import com.turfbook.backend.repository.PlayerSettingsRepository;
import com.turfbook.backend.repository.PushTokenRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Push delivery via the Expo Push API (works for both FCM/Android and APNs/iOS once the app is
 * built with the right credentials — Expo brokers to the native services). Token storage and the
 * preference gate work without any external credentials; only physical delivery requires the
 * client app to be a real build registered for push.
 *
 * <p>Every send is {@code @Async} and best-effort, mirroring {@code MailServiceImpl}: a failure is
 * logged but never propagates to the caller. Sends are skipped entirely when the user has push
 * notifications disabled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;
    private final OwnerSettingsRepository ownerSettingsRepository;
    private final PlayerSettingsRepository playerSettingsRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.push.expo-url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    @Transactional
    public void registerToken(Long userId, String token, String platform) {
        if (token == null || token.isBlank()) return;
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        PushTokenEntity entity = pushTokenRepository.findByToken(token)
                .orElseGet(() -> PushTokenEntity.builder().token(token).build());
        entity.setUser(user);            // re-point an existing token to the latest user
        entity.setPlatform(platform);
        pushTokenRepository.save(entity);
        log.info("PushNotificationService.registerToken() userId={}, platform={}", userId, platform);
    }

    @Override
    @Transactional
    public void unregisterToken(Long userId, String token) {
        if (token == null || token.isBlank()) return;
        pushTokenRepository.deleteByToken(token);
        log.info("PushNotificationService.unregisterToken() userId={}", userId);
    }

    @Override
    @Async("mailExecutor")
    @Transactional(readOnly = true)
    public void sendToUser(Long userId, String title, String body,
                           String referenceId, String referenceType) {
        if (userId == null) return;
        try {
            // Re-load on THIS thread's session — the caller's managed entity must never cross the
            // async boundary (sharing a Hibernate Session across threads corrupts its load state).
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) return;
            if (!isPushEnabled(user)) {
                log.debug("Push suppressed (preference off) for userId={}", user.getId());
                return;
            }

            List<String> tokens = pushTokenRepository.findByUser(user).stream()
                    .map(PushTokenEntity::getToken)
                    .filter(PushNotificationServiceImpl::isExpoToken)
                    .toList();
            if (tokens.isEmpty()) return;

            List<Map<String, Object>> messages = new ArrayList<>();
            for (String to : tokens) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("to", to);
                msg.put("title", title);
                msg.put("body", body);
                msg.put("sound", "default");
                Map<String, Object> data = new HashMap<>();
                if (referenceId != null) data.put("referenceId", referenceId);
                if (referenceType != null) data.put("referenceType", referenceType);
                msg.put("data", data);
                messages.add(msg);
            }

            String payload = objectMapper.writeValueAsString(messages);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(expoPushUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Push sent to {} device(s) for userId={}", tokens.size(), user.getId());
            } else {
                log.warn("Push send returned HTTP {} for userId={}: {}",
                        response.statusCode(), user.getId(), response.body());
            }
        } catch (Exception e) {
            // Best-effort: never let a push failure affect the request flow.
            log.warn("Push send failed for userId={}: {}", userId, e.getMessage());
        }
    }

    /** Resolve the user's push preference by role; default ON when no settings row exists yet. */
    private boolean isPushEnabled(UserEntity user) {
        return switch (user.getRole()) {
            case OWNER -> ownerSettingsRepository.findByOwnerId(user.getId())
                    .map(s -> s.isPushNotificationsEnabled()).orElse(true);
            case PLAYER -> playerSettingsRepository.findByPlayerId(user.getId())
                    .map(s -> s.isPushNotificationsEnabled()).orElse(true);
            case ADMIN -> true;
        };
    }

    private static boolean isExpoToken(String token) {
        return token != null
                && (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["));
    }
}
