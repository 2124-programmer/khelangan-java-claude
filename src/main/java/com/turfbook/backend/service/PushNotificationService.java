package com.turfbook.backend.service;

public interface PushNotificationService {

    /** Register (or re-point) a device push token for a user. */
    void registerToken(Long userId, String token, String platform);

    /** Remove a device push token (e.g. on logout / token rotation). */
    void unregisterToken(Long userId, String token);

    /**
     * Deliver a push to all of a user's devices, honouring their stored push preference.
     * Best-effort + asynchronous: never blocks or fails the caller. A no-op when the user has
     * push disabled or no registered tokens.
     *
     * <p>Takes the user <em>id</em>, not the entity, on purpose: this runs on a separate thread, so
     * it must NOT share a managed entity (and therefore the caller's Hibernate Session) across the
     * async boundary. The user is re-loaded inside this method's own read-only session.
     */
    void sendToUser(Long userId, String title, String body, String referenceId, String referenceType);
}
