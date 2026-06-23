package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.AdminAuditEntity;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.mapper.VenueMapper;
import com.turfbook.backend.repository.AdminAuditRepository;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.SlotRepository;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.AdminOwnerService;
import com.turfbook.backend.service.AuthService;
import com.turfbook.backend.service.NotificationService;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Owners management. The list/detail/moderation machinery mirrors
 * {@link AdminPlayerServiceImpl}; what is owner-specific is the transactional cascade in
 * {@link #suspend}/{@link #ban}/{@link #delete} which unlists venues, cancels upcoming bookings
 * (notifying players — the platform never refunds, payments are direct owner↔player) and voids
 * subscriptions, plus the soft-delete that frees the owner's active_email + active_phone.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOwnerServiceImpl implements AdminOwnerService {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CourtRepository courtRepository;
    private final SlotRepository slotRepository;
    private final AdminAuditRepository auditRepository;
    private final NotificationService notificationService;
    private final AuthService authService;
    private final SubscriptionGate subscriptionGate;
    private final VenueMapper venueMapper;

    @PersistenceContext
    private EntityManager em;

    /** Non-terminal subscription statuses (the ones the cascade freezes/voids). */
    private static final List<SubscriptionStatus> NON_TERMINAL_SUBS =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);
    /** Booking statuses considered "upcoming" and cancellable by the cascade. */
    private static final List<BookingEntity.BookingStatus> UPCOMING_STATUSES =
            List.of(BookingEntity.BookingStatus.PENDING, BookingEntity.BookingStatus.CONFIRMED);

    // ─── List ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OwnerPage listOwners(String q, String segment, String sort, int page, int size) {
        int pg = Math.max(0, page);
        int sz = size <= 0 ? 20 : size;
        String seg = StringUtils.hasText(segment) ? segment.trim().toUpperCase() : "ALL";
        String srt = StringUtils.hasText(sort) ? sort.trim().toUpperCase() : "RECENTLY_ACTIVE";

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder("u.role = :role");
        params.put("role", UserEntity.Role.OWNER);

        if (StringUtils.hasText(q)) {
            where.append(" AND (LOWER(u.name) LIKE :q OR LOWER(u.email) LIKE :q OR u.phone LIKE :qraw"
                    + " OR EXISTS (SELECT 1 FROM VenueEntity v WHERE v.owner = u AND LOWER(v.name) LIKE :q))");
            params.put("q", "%" + q.trim().toLowerCase() + "%");
            params.put("qraw", "%" + q.trim() + "%");
        }

        switch (seg) {
            case "NEW" -> { where.append(" AND u.createdAt >= :since30"); params.put("since30", now.minusDays(30)); }
            case "ONBOARDING" -> where.append(
                    " AND NOT EXISTS (SELECT 1 FROM VenueEntity v WHERE v.owner = u AND v.status = 'LIVE')");
            case "ACTIVE" -> where.append(
                    " AND EXISTS (SELECT 1 FROM VenueEntity v WHERE v.owner = u AND v.status = 'LIVE')");
            case "DORMANT" -> {
                where.append(" AND EXISTS (SELECT 1 FROM VenueEntity v WHERE v.owner = u AND v.status = 'LIVE')");
                where.append(" AND NOT EXISTS (SELECT 1 FROM BookingEntity b WHERE b.venue.owner = u"
                        + " AND b.createdAt >= :since60)");
                params.put("since60", now.minusDays(60));
            }
            case "FLAGGED" -> where.append(
                    " AND (((SELECT COUNT(b) FROM BookingEntity b WHERE b.venue.owner = u) > 0"
                    + " AND (SELECT COUNT(b2) FROM BookingEntity b2 WHERE b2.venue.owner = u"
                    + "   AND b2.status = 'CANCELLED' AND b2.cancellationReason = 'OWNER') * 100"
                    + "   >= 15 * (SELECT COUNT(b3) FROM BookingEntity b3 WHERE b3.venue.owner = u))"
                    + " OR (SELECT AVG(v.ratingAverage) FROM VenueEntity v WHERE v.owner = u) < 3.5)");
            case "RESTRICTED" -> {
                where.append(" AND u.status IN :restricted");
                params.put("restricted", List.of(UserEntity.AccountStatus.SUSPENDED, UserEntity.AccountStatus.BANNED));
            }
            default -> { /* ALL */ }
        }

        String order = switch (srt) {
            case "MOST_VENUES" -> "(SELECT COUNT(v) FROM VenueEntity v WHERE v.owner = u) DESC";
            case "HIGHEST_REVENUE" -> "(SELECT COALESCE(SUM(b.amount),0) FROM BookingEntity b"
                    + " WHERE b.venue.owner = u AND b.paymentStatus = 'SUCCESS') DESC";
            case "RECENTLY_JOINED" -> "u.createdAt DESC";
            case "RATING" -> "(SELECT AVG(v.ratingAverage) FROM VenueEntity v WHERE v.owner = u) DESC";
            case "NAME_ASC" -> "u.name ASC";
            default -> "(SELECT MAX(b.createdAt) FROM BookingEntity b WHERE b.venue.owner = u) DESC"; // RECENTLY_ACTIVE
        };

        TypedQuery<UserEntity> query = em.createQuery(
                "SELECT u FROM UserEntity u WHERE " + where + " ORDER BY " + order, UserEntity.class);
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(u) FROM UserEntity u WHERE " + where, Long.class);
        params.forEach((k, v) -> { query.setParameter(k, v); countQuery.setParameter(k, v); });

        long total = countQuery.getSingleResult();
        query.setFirstResult(pg * sz);
        query.setMaxResults(sz);
        List<UserEntity> owners = query.getResultList();

        List<Long> ids = owners.stream().map(UserEntity::getId).toList();
        Map<Long, long[]> bookingAgg = bookingAggregateFor(ids);
        Map<Long, long[]> venueAgg = venueAggregateFor(ids);

        List<OwnerRow> rows = new ArrayList<>(owners.size());
        for (UserEntity u : owners) {
            rows.add(toRow(u, bookingAgg.get(u.getId()), venueAgg.get(u.getId())));
        }
        int totalPages = (int) Math.ceil((double) total / sz);
        return new OwnerPage().content(rows).totalElements(total).totalPages(totalPages).size(sz).number(pg);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerStats getStats() {
        long total = userRepository.countByRole(UserEntity.Role.OWNER);
        long newThisWeek = userRepository.countByRoleAndCreatedAtAfter(
                UserEntity.Role.OWNER, LocalDateTime.now().minusDays(7));
        long active = venueRepository.countDistinctOwnersWithLiveVenue();
        long onboarding = Math.max(0, total - active);
        long flagged = bookingRepository.countDistinctFlaggedOwners();
        return new OwnerStats()
                .totalOwners((int) total)
                .newThisWeek((int) newThisWeek)
                .activeOwners((int) active)
                .onboardingOwners((int) onboarding)
                .flaggedCount((int) flagged);
    }

    // ─── Detail ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OwnerAdminDetail getDetail(Long ownerId) {
        UserEntity u = requireOwner(ownerId);
        long[] b = bookingAggregateFor(List.of(ownerId)).get(ownerId);
        long[] v = venueAggregateFor(List.of(ownerId)).get(ownerId);

        long gross = b != null ? b[0] : 0;
        long bookingCount = b != null ? b[1] : 0;
        long ownerCancel = b != null ? b[2] : 0;
        long lastActiveEpoch = b != null ? b[3] : 0;
        int totalVenues = v != null ? (int) v[0] : 0;
        int liveVenues = v != null ? (int) v[1] : 0;
        Double rating = ratingFor(ownerId);

        int cancelRatePct = bookingCount > 0 ? (int) Math.round(100.0 * ownerCancel / bookingCount) : 0;
        Risk risk = riskOf(cancelRatePct, rating, bookingCount);

        OwnerStatsBlock stats = new OwnerStatsBlock()
                .totalVenues(totalVenues)
                .liveVenues(liveVenues)
                .grossBookingValue((int) gross)
                .bookingCount((int) bookingCount)
                .rating(rating != null ? BigDecimal.valueOf(rating) : null)
                .ownerCancellationRatePct(cancelRatePct)
                .disputeCount(0)
                .refundRatePct(0);

        OwnerAdminDetail dto = new OwnerAdminDetail()
                .ownerId(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .emailVerified(u.isEmailVerified())
                .phoneVerified(u.isPhoneVerified())
                .status(u.getStatus().name())
                .riskLevel(risk.level())
                .riskReason(risk.reason())
                .joinedAt(odt(u.getCreatedAt()))
                .lastActiveAt(lastActiveEpoch > 0 ? epochToOdt(lastActiveEpoch) : odt(u.getLastActiveAt()))
                .stats(stats)
                .availableActions(availableActions(u.getStatus()));

        if (u.getStatus() == UserEntity.AccountStatus.SUSPENDED && u.getSuspendedReason() != null) {
            dto.suspension(new OwnerSuspensionInfo()
                    .reason(u.getSuspendedReason())
                    .until(u.getSuspendedUntil()));
        }
        if (u.getStatus() == UserEntity.AccountStatus.DELETED) {
            String byName = u.getDeletedBy() != null
                    ? userRepository.findById(u.getDeletedBy()).map(UserEntity::getName).orElse(null) : null;
            dto.deletion(new OwnerDeletionInfo()
                    .deletedAt(odt(u.getDeletedAt()))
                    .deletedByName(byName)
                    .reason(u.getDeletedReason())
                    .venuesArchived(u.getDeletedVenuesArchived())
                    .bookingsCancelled(u.getDeletedBookingsCancelled())
                    .subscriptionsVoided(u.getDeletedSubscriptionsVoided()));
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSummaryPage getVenues(Long ownerId, int page, int size) {
        UserEntity u = requireOwner(ownerId);
        Pageable pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 20 : size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var entityPage = venueRepository.findByOwner(u, pageable);
        return new VenueSummaryPage()
                .content(entityPage.getContent().stream().map(venueMapper::toSummaryDto).toList())
                .totalElements(entityPage.getTotalElements())
                .totalPages(entityPage.getTotalPages())
                .size(entityPage.getSize())
                .number(entityPage.getNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueSubscriptionRow> getSubscriptions(Long ownerId) {
        UserEntity u = requireOwner(ownerId);
        List<VenueSubscriptionRow> rows = new ArrayList<>();
        for (VenueEntity venue : venueRepository.findByOwner(u)) {
            SubscriptionEntity sub = subscriptionRepository
                    .findFirstByVenueAndStatusInOrderByIdDesc(venue, NON_TERMINAL_SUBS)
                    .orElse(null);
            rows.add(buildSubscriptionRow(venue, u, sub));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerBookingPage getBookings(Long ownerId, int page, int size) {
        UserEntity u = requireOwner(ownerId);
        Pageable pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 20 : size);
        var bookingPage = bookingRepository.findByVenueOwner(u, null, pageable);
        List<OwnerBookingRow> rows = bookingPage.getContent().stream().map(b -> new OwnerBookingRow()
                .bookingId(b.getId())
                .venueName(b.getVenue() != null ? b.getVenue().getName() : "—")
                .playerName(b.getPlayer() != null ? b.getPlayer().getName() : "—")
                .date(b.getDate())
                .slotLabel(slotLabel(b))
                .amount(b.getAmount())
                .status(b.getStatus() != null ? b.getStatus().name() : "")).toList();
        return new OwnerBookingPage()
                .content(rows)
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .size(bookingPage.getSize())
                .number(bookingPage.getNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerAuditPage getAudit(Long ownerId, int page, int size) {
        requireOwner(ownerId);
        Pageable pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 20 : size);
        var auditPage = auditRepository.findByTargetId(ownerId, pageable);
        List<PlayerAuditRow> rows = auditPage.getContent().stream().map(a -> new PlayerAuditRow()
                .id(a.getId())
                .actorName(a.getActor() != null ? a.getActor().getName() : null)
                .action(a.getAction())
                .reason(a.getReason())
                .fromStatus(a.getFromStatus())
                .toStatus(a.getToStatus())
                .createdAt(odt(a.getCreatedAt()))).toList();
        return new PlayerAuditPage()
                .content(rows)
                .totalElements(auditPage.getTotalElements())
                .totalPages(auditPage.getTotalPages())
                .size(auditPage.getSize())
                .number(auditPage.getNumber());
    }

    // ─── Moderation actions ──────────────────────────────────────────────────

    @Override
    @Transactional
    public OwnerAdminDetail suspend(Long ownerId, OwnerSuspendBody body, Long actorId) {
        if (body == null || !StringUtils.hasText(body.getReason())) {
            throw new BadRequestException("A reason is required to suspend.");
        }
        UserEntity u = requireOwner(ownerId);
        requireNotDeleted(u);
        if (u.getStatus() != UserEntity.AccountStatus.ACTIVE) {
            throw new ConflictException("Only an active owner can be suspended.");
        }
        String from = u.getStatus().name();
        int unlisted = unlistLiveVenues(u);
        u.setStatus(UserEntity.AccountStatus.SUSPENDED);
        u.setBlocked(true);
        u.setSuspendedReason(body.getReason());
        u.setSuspendedUntil(body.getUntil());
        userRepository.save(u);
        audit(actorId, u, "SUSPEND", body.getReason(), from, u.getStatus().name(),
                "venuesUnlisted=" + unlisted);
        return getDetail(ownerId);
    }

    @Override
    @Transactional
    public OwnerAdminDetail reactivate(Long ownerId, Long actorId) {
        UserEntity u = requireOwner(ownerId);
        requireNotDeleted(u);
        if (u.getStatus() != UserEntity.AccountStatus.SUSPENDED && u.getStatus() != UserEntity.AccountStatus.BANNED) {
            throw new ConflictException("Only a suspended or banned owner can be reactivated.");
        }
        String from = u.getStatus().name();
        int relisted = relistOwnerVenues(u);
        u.setStatus(UserEntity.AccountStatus.ACTIVE);
        u.setBlocked(false);
        u.setSuspendedReason(null);
        u.setSuspendedUntil(null);
        userRepository.save(u);
        audit(actorId, u, "REACTIVATE", null, from, u.getStatus().name(), "venuesRelisted=" + relisted);
        return getDetail(ownerId);
    }

    @Override
    @Transactional
    public OwnerAdminDetail ban(Long ownerId, OwnerBanBody body, Long actorId) {
        requireModerateHard(actorId);
        if (body == null || !StringUtils.hasText(body.getReason())) {
            throw new BadRequestException("A reason is required to ban.");
        }
        UserEntity u = requireOwner(ownerId);
        requireNotDeleted(u);
        if (u.getStatus() != UserEntity.AccountStatus.ACTIVE && u.getStatus() != UserEntity.AccountStatus.SUSPENDED) {
            throw new ConflictException("Only an active or suspended owner can be banned.");
        }
        String from = u.getStatus().name();
        int unlisted = unlistLiveVenues(u);
        int cancelled = 0;
        if (Boolean.TRUE.equals(body.getCancelUpcomingBookings())) {
            cancelled = cancelUpcomingBookings(u, "the venue owner's account was banned");
        }
        u.setStatus(UserEntity.AccountStatus.BANNED);
        u.setBlocked(true);
        u.setSuspendedReason(body.getReason());
        u.setTokenVersion(u.getTokenVersion() + 1); // kick out live sessions
        userRepository.save(u);
        audit(actorId, u, "BAN", body.getReason(), from, u.getStatus().name(),
                "venuesUnlisted=" + unlisted + ",bookingsCancelled=" + cancelled);
        return getDetail(ownerId);
    }

    @Override
    @Transactional
    public OwnerAdminDetail unban(Long ownerId, Long actorId) {
        requireModerateHard(actorId);
        UserEntity u = requireOwner(ownerId);
        requireNotDeleted(u);
        if (u.getStatus() != UserEntity.AccountStatus.BANNED) {
            throw new ConflictException("Only a banned owner can be unbanned.");
        }
        String from = u.getStatus().name();
        int relisted = relistOwnerVenues(u);
        u.setStatus(UserEntity.AccountStatus.ACTIVE);
        u.setBlocked(false);
        u.setSuspendedReason(null);
        userRepository.save(u);
        audit(actorId, u, "UNBAN", null, from, u.getStatus().name(), "venuesRelisted=" + relisted);
        return getDetail(ownerId);
    }

    @Override
    @Transactional
    public OwnerAdminDetail setVerification(Long ownerId, OwnerVerificationBody body, Long actorId) {
        UserEntity u = requireOwner(ownerId);
        requireNotDeleted(u);
        String channel = body != null && body.getChannel() != null ? body.getChannel().toUpperCase() : "";
        boolean verified = body != null && Boolean.TRUE.equals(body.getVerified());
        switch (channel) {
            case "EMAIL" -> u.setEmailVerified(verified);
            case "PHONE" -> u.setPhoneVerified(verified);
            default -> throw new BadRequestException("channel must be EMAIL or PHONE");
        }
        userRepository.save(u);
        audit(actorId, u, verified ? "VERIFY" : "UNVERIFY", channel, null, null, null);
        return getDetail(ownerId);
    }

    @Override
    @Transactional
    public void forceLogout(Long ownerId, Long actorId) {
        UserEntity u = requireOwner(ownerId);
        requireNotDeleted(u);
        u.setTokenVersion(u.getTokenVersion() + 1);
        userRepository.save(u);
        audit(actorId, u, "FORCE_LOGOUT", null, null, null, null);
    }

    @Override
    @Transactional
    public void triggerPasswordReset(Long ownerId, Long actorId) {
        UserEntity u = requireOwner(ownerId);
        requireNotDeleted(u);
        try {
            PasswordResetRequest req = new PasswordResetRequest();
            req.setEmail(u.getActiveEmail() != null ? u.getActiveEmail() : u.getEmail());
            authService.requestPasswordReset(req); // dispatches OTP/link; returns no secret
        } catch (Exception e) {
            log.warn("Password reset dispatch failed for owner {}: {}", ownerId, e.getMessage());
        }
        audit(actorId, u, "RESET_PASSWORD", null, null, null, null);
    }

    @Override
    @Transactional
    public void message(Long ownerId, OwnerMessageBody body, Long actorId) {
        if (body == null || body.getChannels() == null || body.getChannels().isEmpty()) {
            throw new BadRequestException("At least one channel is required.");
        }
        if (!StringUtils.hasText(body.getBody())) throw new BadRequestException("Message body is required.");
        UserEntity u = requireOwner(ownerId);
        requireNotDeleted(u);
        String title = StringUtils.hasText(body.getSubject()) ? body.getSubject() : "Message from Score-Adda";
        // IN_APP is delivered directly; EMAIL/SMS are best-effort (logged) — no generic sender yet.
        if (body.getChannels().stream().anyMatch(c -> "IN_APP".equalsIgnoreCase(c))) {
            notificationService.createNotification(u, title, body.getBody(),
                    NotificationEntity.NotificationType.SYSTEM);
        }
        audit(actorId, u, "MESSAGE", String.join(",", body.getChannels()), null, null, null);
    }

    @Override
    @Transactional
    public OwnerAdminDetail delete(Long ownerId, OwnerReasonBody body, Long actorId) {
        requireModerateHard(actorId);
        if (body == null || !StringUtils.hasText(body.getReason())) {
            throw new BadRequestException("A reason is required to delete an owner.");
        }
        UserEntity u = requireOwner(ownerId);
        if (u.getStatus() == UserEntity.AccountStatus.DELETED) {
            throw new ConflictException("This owner is already deleted.");
        }
        String from = u.getStatus().name();

        // 1) Archive every venue (removed from the marketplace, terminal).
        int venuesArchived = 0;
        List<VenueEntity> venues = venueRepository.findByOwner(u);
        for (VenueEntity venue : venues) {
            if (venue.getStatus() != VenueEntity.VenueStatus.ARCHIVED) {
                venue.setStatus(VenueEntity.VenueStatus.ARCHIVED);
                venue.setUnlistedByOwner(false);
                venue.setActive(false);
                venueRepository.save(venue);
                subscriptionGate.recomputeVenueLiveFlag(venue.getId());
                venuesArchived++;
            }
        }

        // 2) Cancel all upcoming bookings + notify players (no platform refund).
        int bookingsCancelled = cancelUpcomingBookings(u, "the venue is no longer available");

        // 3) Void the owner's non-terminal subscriptions.
        int subscriptionsVoided = 0;
        for (SubscriptionEntity sub : subscriptionRepository.findByOwner_IdAndStatusIn(ownerId, NON_TERMINAL_SUBS)) {
            sub.setStatus(SubscriptionStatus.VOIDED);
            subscriptionRepository.save(sub);
            subscriptionGate.recomputeVenueLiveFlag(sub.getVenue().getId());
            subscriptionsVoided++;
        }

        // 4) Soft-delete the owner: keep email/phone/name for history, free active_* for reuse.
        u.setStatus(UserEntity.AccountStatus.DELETED);
        u.setBlocked(true);
        u.setTokenVersion(u.getTokenVersion() + 1);
        u.setActiveEmail(null);
        u.setActivePhone(null);
        u.setSuspendedReason(null);
        u.setSuspendedUntil(null);
        u.setDeletedAt(LocalDateTime.now());
        u.setDeletedBy(actorId);
        u.setDeletedReason(body.getReason());
        u.setDeletedVenuesArchived(venuesArchived);
        u.setDeletedBookingsCancelled(bookingsCancelled);
        u.setDeletedSubscriptionsVoided(subscriptionsVoided);
        userRepository.save(u);

        audit(actorId, u, "DELETE", body.getReason(), from, u.getStatus().name(),
                "venuesArchived=" + venuesArchived + ",bookingsCancelled=" + bookingsCancelled
                        + ",subscriptionsVoided=" + subscriptionsVoided);
        log.info("Owner {} soft-deleted by admin {} — archived {} venue(s), cancelled {} booking(s), voided {} sub(s).",
                ownerId, actorId, venuesArchived, bookingsCancelled, subscriptionsVoided);
        return getDetail(ownerId);
    }

    // ─── Cascade helpers ─────────────────────────────────────────────────────

    /** Unlist (LIVE → SUSPENDED) the owner's live venues, tagging them so reactivation can relist them. */
    private int unlistLiveVenues(UserEntity owner) {
        int n = 0;
        for (VenueEntity venue : venueRepository.findByOwner(owner)) {
            if (venue.getStatus() == VenueEntity.VenueStatus.LIVE) {
                venue.setStatus(VenueEntity.VenueStatus.SUSPENDED);
                venue.setUnlistedByOwner(true);
                venueRepository.save(venue);
                subscriptionGate.recomputeVenueLiveFlag(venue.getId());
                n++;
            }
        }
        return n;
    }

    /** Relist exactly the venues an owner suspension/ban hid (back to LIVE; gate decides visibility). */
    private int relistOwnerVenues(UserEntity owner) {
        int n = 0;
        for (VenueEntity venue : venueRepository.findByOwner(owner)) {
            if (venue.isUnlistedByOwner() && venue.getStatus() == VenueEntity.VenueStatus.SUSPENDED) {
                venue.setStatus(VenueEntity.VenueStatus.LIVE);
                venue.setUnlistedByOwner(false);
                venueRepository.save(venue);
                subscriptionGate.recomputeVenueLiveFlag(venue.getId()); // active sub ⇒ visible; else stays dark
                n++;
            }
        }
        return n;
    }

    /** Cancel upcoming bookings across the owner's venues and notify each player. Returns the count. */
    private int cancelUpcomingBookings(UserEntity owner, String playerReasonPhrase) {
        List<BookingEntity> upcoming = bookingRepository.findUpcomingForOwner(
                owner, UPCOMING_STATUSES, LocalDate.now());
        int n = 0;
        for (BookingEntity b : upcoming) {
            b.setStatus(BookingEntity.BookingStatus.CANCELLED);
            b.setCancellationReason(BookingEntity.CancellationReason.OWNER);
            SlotEntity slot = b.getSlot();
            if (slot != null) {
                slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
                slotRepository.save(slot);
            }
            bookingRepository.save(b);
            if (b.getPlayer() != null) {
                notificationService.createNotification(
                        b.getPlayer(),
                        "Booking Cancelled",
                        String.format("Your booking at %s on %s (%s) has been cancelled because %s. "
                                + "Any payment is settled directly with the venue.",
                                b.getVenue() != null ? b.getVenue().getName() : "the venue",
                                b.getDate(), slotLabel(b), playerReasonPhrase),
                        NotificationEntity.NotificationType.BOOKING,
                        String.valueOf(b.getId()), "BOOKING");
            }
            n++;
        }
        return n;
    }

    // ─── Aggregates / mapping ────────────────────────────────────────────────

    /** ownerId → [grossBookingValue(SUCCESS), bookingCount, ownerCancelCount, lastBookingEpochSec]. */
    private Map<Long, long[]> bookingAggregateFor(List<Long> ids) {
        Map<Long, long[]> out = new HashMap<>();
        if (ids == null || ids.isEmpty()) return out;
        for (Object[] r : bookingRepository.aggregateByOwnerIds(ids)) {
            Long id = ((Number) r[0]).longValue();
            long gross = r[1] != null ? ((Number) r[1]).longValue() : 0;
            long count = r[2] != null ? ((Number) r[2]).longValue() : 0;
            long cancels = r[3] != null ? ((Number) r[3]).longValue() : 0;
            long lastEpoch = r[4] instanceof LocalDateTime ldt ? ldt.toEpochSecond(ZoneOffset.UTC) : 0;
            out.put(id, new long[]{ gross, count, cancels, lastEpoch });
        }
        return out;
    }

    /** ownerId → [totalVenues, liveVenues, avgRatingX1000] (rating scaled to keep the long[] integral). */
    private Map<Long, long[]> venueAggregateFor(List<Long> ids) {
        Map<Long, long[]> out = new HashMap<>();
        if (ids == null || ids.isEmpty()) return out;
        for (Object[] r : venueRepository.aggregateByOwnerIds(ids)) {
            Long id = ((Number) r[0]).longValue();
            long total = r[1] != null ? ((Number) r[1]).longValue() : 0;
            long live = r[2] != null ? ((Number) r[2]).longValue() : 0;
            long ratingX1000 = r[3] != null ? Math.round(((Number) r[3]).doubleValue() * 1000) : -1;
            out.put(id, new long[]{ total, live, ratingX1000 });
        }
        return out;
    }

    private Double ratingFor(Long ownerId) {
        long[] v = venueAggregateFor(List.of(ownerId)).get(ownerId);
        if (v == null || v[2] < 0) return null;
        return v[2] / 1000.0;
    }

    private OwnerRow toRow(UserEntity u, long[] b, long[] v) {
        long gross = b != null ? b[0] : 0;
        long bookingCount = b != null ? b[1] : 0;
        long ownerCancel = b != null ? b[2] : 0;
        long lastEpoch = b != null ? b[3] : 0;
        int totalVenues = v != null ? (int) v[0] : 0;
        int liveVenues = v != null ? (int) v[1] : 0;
        Double rating = v != null && v[2] >= 0 ? v[2] / 1000.0 : null;
        int cancelRatePct = bookingCount > 0 ? (int) Math.round(100.0 * ownerCancel / bookingCount) : 0;
        Risk risk = riskOf(cancelRatePct, rating, bookingCount);

        return new OwnerRow()
                .ownerId(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .emailVerified(u.isEmailVerified())
                .phoneVerified(u.isPhoneVerified())
                .status(u.getStatus().name())
                .riskLevel(risk.level())
                .riskReason(risk.reason())
                .totalVenues(totalVenues)
                .liveVenues(liveVenues)
                .grossBookingValue((int) gross)
                .rating(rating != null ? BigDecimal.valueOf(rating) : null)
                .lastActiveAt(lastEpoch > 0 ? epochToOdt(lastEpoch) : odt(u.getLastActiveAt()))
                .joinedAt(odt(u.getCreatedAt()));
    }

    private VenueSubscriptionRow buildSubscriptionRow(VenueEntity venue, UserEntity owner, SubscriptionEntity sub) {
        VenueSubscriptionRow row = new VenueSubscriptionRow()
                .venueId(venue.getId())
                .venueName(venue.getName())
                .venueCity(venue.getCity())
                .ownerName(owner.getName())
                .ownerMobile(owner.getPhone())
                .courtsUsed((int) courtRepository.countByVenue(venue));
        if (sub != null) {
            row.currentPlanCode(sub.getPlanCode() != null ? sub.getPlanCode().name() : null)
               .currentPlanName(sub.getPlanName())
               .currentStatus(rollupStatus(sub))
               .endDate(odt(sub.getPeriodEnd()))
               .courtLimit(sub.getMaxCourts());
        } else {
            row.currentStatus("NONE");
        }
        return row;
    }

    /** Row-level rollup for the shared VenueSubscriptionRow card. */
    private String rollupStatus(SubscriptionEntity sub) {
        return switch (sub.getStatus()) {
            case TRIALING -> "TRIAL";
            case ACTIVE -> sub.getPeriodEnd() != null
                    && sub.getPeriodEnd().isBefore(LocalDateTime.now().plusDays(7)) ? "EXPIRING" : "ACTIVE";
            case PAST_DUE, EXPIRED -> "EXPIRED";
            default -> "NONE";
        };
    }

    private record Risk(String level, String reason) {}

    /** Owner risk from owner-cancellation rate + venue rating (no dispute system yet). */
    private Risk riskOf(int cancelRatePct, Double rating, long bookingCount) {
        if (bookingCount > 0 && cancelRatePct >= 30) {
            return new Risk("HIGH", cancelRatePct + "% owner-cancellation rate");
        }
        if (rating != null && rating < 2.5) {
            return new Risk("HIGH", "Low rating (" + String.format("%.1f", rating) + "★)");
        }
        if (bookingCount > 0 && cancelRatePct >= 15) {
            return new Risk("MEDIUM", cancelRatePct + "% owner-cancellation rate");
        }
        if (rating != null && rating < 3.5) {
            return new Risk("MEDIUM", "Below-average rating (" + String.format("%.1f", rating) + "★)");
        }
        return new Risk("NONE", null);
    }

    private List<String> availableActions(UserEntity.AccountStatus status) {
        return switch (status) {
            case ACTIVE -> List.of("SUSPEND", "BAN", "VERIFY", "UNVERIFY",
                    "FORCE_LOGOUT", "RESET_PASSWORD", "MESSAGE", "DELETE");
            case SUSPENDED -> List.of("REACTIVATE", "BAN", "MESSAGE", "DELETE");
            case BANNED -> List.of("UNBAN", "MESSAGE", "DELETE");
            case DELETED -> List.of();
        };
    }

    // ─── Small utilities ─────────────────────────────────────────────────────

    private UserEntity requireOwner(Long ownerId) {
        UserEntity u = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", ownerId));
        if (u.getRole() != UserEntity.Role.OWNER) {
            throw new ResourceNotFoundException("Owner", "id", ownerId);
        }
        return u;
    }

    private void requireNotDeleted(UserEntity u) {
        if (u.getStatus() == UserEntity.AccountStatus.DELETED) {
            throw new ConflictException("This owner has been deleted and is read-only.");
        }
    }

    /** Phase 1 RBAC: every ADMIN may moderate hard (ban/delete). SUPER_ADMIN/SUPPORT sub-roles slot in later. */
    private void requireModerateHard(Long actorId) {
        boolean canModerateHard = true;
        if (!canModerateHard) throw new ForbiddenException("You do not have permission for this action.");
    }

    private void audit(Long actorId, UserEntity target, String action, String reason,
                       String from, String to, String metadata) {
        UserEntity actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
        auditRepository.save(AdminAuditEntity.builder()
                .actor(actor).target(target).action(action).reason(reason)
                .fromStatus(from).toStatus(to).metadata(metadata).build());
    }

    private static String slotLabel(BookingEntity b) {
        if (b.getStartTime() == null || b.getEndTime() == null) return "";
        return b.getStartTime() + " – " + b.getEndTime();
    }

    private static OffsetDateTime odt(LocalDateTime ldt) {
        return ldt != null ? ldt.atOffset(ZoneOffset.UTC) : null;
    }

    private static OffsetDateTime epochToOdt(long epochSec) {
        return OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(epochSec), ZoneOffset.UTC);
    }
}
