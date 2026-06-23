package com.turfbook.backend.service;

import com.turfbook.backend.dto.OwnerAdminDetail;
import com.turfbook.backend.dto.OwnerBanBody;
import com.turfbook.backend.dto.OwnerBookingPage;
import com.turfbook.backend.dto.OwnerMessageBody;
import com.turfbook.backend.dto.OwnerPage;
import com.turfbook.backend.dto.OwnerReasonBody;
import com.turfbook.backend.dto.OwnerStats;
import com.turfbook.backend.dto.OwnerSuspendBody;
import com.turfbook.backend.dto.OwnerVerificationBody;
import com.turfbook.backend.dto.PlayerAuditPage;
import com.turfbook.backend.dto.VenueSubscriptionRow;
import com.turfbook.backend.dto.VenueSummaryPage;

import java.util.List;

/**
 * Admin surface for managing venue owners (the supply side). Mirrors {@link AdminPlayerService}
 * but its actions carry blast radius: suspend/ban unlist the owner's venues, and delete archives
 * venues, cancels upcoming bookings, voids subscriptions and frees the owner's email + phone.
 */
public interface AdminOwnerService {

    OwnerPage listOwners(String q, String segment, String sort, int page, int size);

    OwnerStats getStats();

    OwnerAdminDetail getDetail(Long ownerId);

    VenueSummaryPage getVenues(Long ownerId, int page, int size);

    List<VenueSubscriptionRow> getSubscriptions(Long ownerId);

    OwnerBookingPage getBookings(Long ownerId, int page, int size);

    PlayerAuditPage getAudit(Long ownerId, int page, int size);

    OwnerAdminDetail suspend(Long ownerId, OwnerSuspendBody body, Long actorId);

    OwnerAdminDetail reactivate(Long ownerId, Long actorId);

    OwnerAdminDetail ban(Long ownerId, OwnerBanBody body, Long actorId);

    OwnerAdminDetail unban(Long ownerId, Long actorId);

    OwnerAdminDetail setVerification(Long ownerId, OwnerVerificationBody body, Long actorId);

    void forceLogout(Long ownerId, Long actorId);

    void triggerPasswordReset(Long ownerId, Long actorId);

    void message(Long ownerId, OwnerMessageBody body, Long actorId);

    OwnerAdminDetail delete(Long ownerId, OwnerReasonBody body, Long actorId);
}
