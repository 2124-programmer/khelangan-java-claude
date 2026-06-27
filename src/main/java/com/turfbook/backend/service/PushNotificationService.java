package com.turfbook.backend.service;

import com.turfbook.backend.entity.UserEntity;

public interface PushNotificationService {

    /** Register (or re-point) a device push token for a user. */
    void registerToken(Long userId, String token, String platform);

    /** Remove a device push token (e.g. on logout / token rotation). */
    void unregisterToken(Long userId, String token);

    /**
     * Deliver a push to all of a user's devices, honouring their stored push preference.
     * Best-effort + asynchronous: never blocks or fails the caller. A no-op when the user has
     * push disabled or no registered tokens.
     */
    void sendToUser(UserEntity user, String title, String body, String referenceId, String referenceType);
}
