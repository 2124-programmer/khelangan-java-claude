package com.turfbook.backend.service;

import com.turfbook.backend.dto.PlayerAdminDetail;
import com.turfbook.backend.dto.PlayerAuditPage;
import com.turfbook.backend.dto.PlayerBookingPage;
import com.turfbook.backend.dto.PlayerMessageBody;
import com.turfbook.backend.dto.PlayerPage;
import com.turfbook.backend.dto.PlayerPaymentPage;
import com.turfbook.backend.dto.PlayerReasonBody;
import com.turfbook.backend.dto.PlayerStats;
import com.turfbook.backend.dto.PlayerSuspendBody;
import com.turfbook.backend.dto.PlayerVerificationBody;

/** Admin-facing player management at scale (list, detail, moderation, audit). */
public interface AdminPlayerService {

    PlayerPage listPlayers(String q, String segment, String sort, int page, int size);

    PlayerStats getStats();

    PlayerAdminDetail getDetail(Long playerId);

    PlayerAdminDetail suspend(Long playerId, PlayerSuspendBody body, Long actorId);

    PlayerAdminDetail reactivate(Long playerId, Long actorId);

    PlayerAdminDetail ban(Long playerId, PlayerReasonBody body, Long actorId);

    PlayerAdminDetail unban(Long playerId, Long actorId);

    /** Soft-delete a player (SUPER_ADMIN only): cancels upcoming bookings, frees active_email/phone. */
    PlayerAdminDetail delete(Long playerId, PlayerReasonBody body, Long actorId);

    PlayerAdminDetail setVerification(Long playerId, PlayerVerificationBody body, Long actorId);

    void forceLogout(Long playerId, Long actorId);

    void triggerPasswordReset(Long playerId, Long actorId);

    void message(Long playerId, PlayerMessageBody body, Long actorId);

    PlayerBookingPage getBookings(Long playerId, int page, int size);

    PlayerPaymentPage getPayments(Long playerId, int page, int size);

    PlayerAuditPage getAudit(Long playerId, int page, int size);
}
