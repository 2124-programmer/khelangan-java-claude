package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.AdminAuditEntity;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.AdminAuditRepository;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.ReviewRepository;
import com.turfbook.backend.repository.SlotRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.AdminPermissionService;
import com.turfbook.backend.service.AdminPlayerService;
import com.turfbook.backend.service.AuthService;
import com.turfbook.backend.service.NotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlayerServiceImpl implements AdminPlayerService {

    private static final int RESTRICT_NOSHOW_HIGH = 3;
    private static final int FLAG_PLAYER_CANCEL = 3;

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final AdminAuditRepository auditRepository;
    private final NotificationService notificationService;
    private final AuthService authService;
    private final AdminPermissionService adminPermissionService;
    private final SlotRepository slotRepository;

    /** Statuses considered "upcoming" when cancelling a deleted player's bookings. */
    private static final List<BookingEntity.BookingStatus> UPCOMING_STATUSES =
            List.of(BookingEntity.BookingStatus.PENDING, BookingEntity.BookingStatus.CONFIRMED);

    @PersistenceContext
    private EntityManager em;

    // ═══════════════════════════════════════════════════════════════════════
    //  List + stats
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PlayerPage listPlayers(String q, String segment, String sort, int page, int size) {
        int pg = Math.max(0, page);
        int sz = size <= 0 ? 20 : size;
        String seg = StringUtils.hasText(segment) ? segment.trim().toUpperCase() : "ALL";
        String srt = StringUtils.hasText(sort) ? sort.trim().toUpperCase() : "RECENTLY_ACTIVE";

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder("u.role = :role");
        params.put("role", UserEntity.Role.PLAYER);

        if (StringUtils.hasText(q)) {
            where.append(" AND (LOWER(u.name) LIKE :q OR LOWER(u.email) LIKE :q OR u.phone LIKE :qraw)");
            params.put("q", "%" + q.trim().toLowerCase() + "%");
            params.put("qraw", "%" + q.trim() + "%");
        }

        switch (seg) {
            case "NEW" -> { where.append(" AND u.createdAt >= :since30"); params.put("since30", now.minusDays(30)); }
            case "ACTIVE" -> {
                where.append(" AND EXISTS (SELECT 1 FROM BookingEntity b WHERE b.player = u AND b.createdAt >= :since30)");
                params.put("since30", now.minusDays(30));
            }
            case "DORMANT" -> {
                where.append(" AND NOT EXISTS (SELECT 1 FROM BookingEntity b WHERE b.player = u AND b.createdAt >= :since60)");
                params.put("since60", now.minusDays(60));
            }
            case "FLAGGED" -> {
                where.append(" AND ((SELECT COUNT(b) FROM BookingEntity b WHERE b.player = u AND b.cancellationReason = :timeOver) >= 1"
                        + " OR (SELECT COUNT(b) FROM BookingEntity b WHERE b.player = u AND b.status = :cancelled AND b.cancellationReason = :playerReason) >= :flagCancel)");
                params.put("timeOver", BookingEntity.CancellationReason.TIME_OVER);
                params.put("cancelled", BookingEntity.BookingStatus.CANCELLED);
                params.put("playerReason", BookingEntity.CancellationReason.PLAYER);
                params.put("flagCancel", (long) FLAG_PLAYER_CANCEL);
            }
            case "RESTRICTED" -> {
                where.append(" AND u.status IN :restricted");
                params.put("restricted", List.of(UserEntity.AccountStatus.SUSPENDED, UserEntity.AccountStatus.BANNED));
            }
            default -> { /* ALL */ }
        }

        String order = switch (srt) {
            case "MOST_BOOKINGS" -> "u.totalBookings DESC";
            case "HIGHEST_SPEND" -> "(SELECT COALESCE(SUM(b.amount),0) FROM BookingEntity b WHERE b.player = u AND b.paymentStatus = :success) DESC";
            case "RECENTLY_JOINED" -> "u.createdAt DESC";
            case "NAME_ASC" -> "u.name ASC";
            default -> "(SELECT MAX(b.createdAt) FROM BookingEntity b WHERE b.player = u) DESC"; // RECENTLY_ACTIVE
        };
        if (order.contains(":success")) params.put("success", BookingEntity.PaymentStatus.SUCCESS);

        TypedQuery<UserEntity> query = em.createQuery(
                "SELECT u FROM UserEntity u WHERE " + where + " ORDER BY " + order, UserEntity.class);
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(u) FROM UserEntity u WHERE " + where, Long.class);
        params.forEach((k, v) -> {
            // The order-by spend param only exists on the data query.
            if (!"success".equals(k)) countQuery.setParameter(k, v);
            query.setParameter(k, v);
        });

        long total = countQuery.getSingleResult();
        query.setFirstResult(pg * sz);
        query.setMaxResults(sz);
        List<UserEntity> users = query.getResultList();

        Map<Long, long[]> agg = aggregateFor(users.stream().map(UserEntity::getId).toList());
        List<PlayerRow> rows = new ArrayList<>(users.size());
        for (UserEntity u : users) {
            rows.add(toRow(u, agg.get(u.getId())));
        }
        int totalPages = (int) Math.ceil((double) total / sz);
        return new PlayerPage().content(rows).totalElements(total).totalPages(totalPages).size(sz).number(pg);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerStats getStats() {
        LocalDateTime now = LocalDateTime.now();
        long totalPlayers = userRepository.countByRole(UserEntity.Role.PLAYER);
        long newThisWeek = userRepository.countByRoleAndCreatedAtAfter(UserEntity.Role.PLAYER, now.minusDays(7));
        long activeBooked = bookingRepository.countDistinctPlayersBookedSince(now.minusDays(30));
        int activeRate = totalPlayers > 0 ? (int) Math.round(100.0 * activeBooked / totalPlayers) : 0;
        long flagged = bookingRepository.countFlaggedPlayers();
        return new PlayerStats()
                .totalPlayers((int) totalPlayers)
                .newThisWeek((int) newThisWeek)
                .activeRatePct(activeRate)
                .flaggedCount((int) flagged);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Detail
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PlayerAdminDetail getDetail(Long playerId) {
        UserEntity u = requireUser(playerId);
        long[] a = aggregateFor(List.of(playerId)).getOrDefault(playerId, new long[6]);
        long bookingCount = a[2], playerCancels = a[3], noShows = a[4];
        List<Object[]> refundRows = bookingRepository.refundAggregateForPlayer(playerId);
        Object[] refund = refundRows.isEmpty() ? new Object[]{0L, 0L} : refundRows.get(0);
        long refundCount = refund[0] != null ? ((Number) refund[0]).longValue() : 0;
        long refundTotal = refund[1] != null ? ((Number) refund[1]).longValue() : 0;
        long reviewCount = reviewRepository.countByPlayer(u);
        int cancelRate = bookingCount > 0 ? (int) Math.round(100.0 * playerCancels / bookingCount) : 0;

        PlayerStatsBlock stats = new PlayerStatsBlock()
                .bookingCount((int) bookingCount)
                .totalSpend((int) a[1])
                .refundCount((int) refundCount)
                .refundTotal((int) refundTotal)
                .reviewCount((int) reviewCount)
                .cancellationRatePct(cancelRate)
                .noShowCount((int) noShows);

        Risk risk = riskOf(bookingCount, playerCancels, noShows, u.getDisputeAtFaultCount());

        OffsetDateTime lastActive = a[0] > 0
                ? OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(a[0]), ZoneOffset.UTC)
                : odt(u.getLastActiveAt());

        PlayerAdminDetail dto = new PlayerAdminDetail()
                .playerId(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .emailVerified(u.isEmailVerified())
                .phoneVerified(u.isPhoneVerified())
                .status(u.getStatus().name())
                .riskLevel(risk.level)
                .riskReason(risk.reason)
                .joinedAt(odt(u.getCreatedAt()))
                .lastActiveAt(lastActive)
                .stats(stats)
                .availableActions(availableActions(u.getStatus()));

        if (u.getStatus() == UserEntity.AccountStatus.SUSPENDED) {
            dto.setSuspension(new PlayerSuspensionInfo()
                    .reason(u.getSuspendedReason())
                    .until(u.getSuspendedUntil()));
        }
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Moderation actions
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public PlayerAdminDetail suspend(Long playerId, PlayerSuspendBody body, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        if (!StringUtils.hasText(body.getReason())) throw new BadRequestException("A reason is required to suspend.");
        UserEntity u = requireUser(playerId);
        if (u.getStatus() != UserEntity.AccountStatus.ACTIVE) {
            throw new ConflictException("Only an active player can be suspended.");
        }
        String from = u.getStatus().name();
        u.setStatus(UserEntity.AccountStatus.SUSPENDED);
        u.setBlocked(true);
        u.setSuspendedReason(body.getReason());
        u.setSuspendedUntil(body.getUntil());
        userRepository.save(u);
        audit(actorId, u, "SUSPEND", body.getReason(), from, u.getStatus().name());
        return getDetail(playerId);
    }

    @Override
    @Transactional
    public PlayerAdminDetail reactivate(Long playerId, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        UserEntity u = requireUser(playerId);
        if (u.getStatus() != UserEntity.AccountStatus.SUSPENDED && u.getStatus() != UserEntity.AccountStatus.BANNED) {
            throw new ConflictException("Only a suspended or banned player can be reactivated.");
        }
        String from = u.getStatus().name();
        u.setStatus(UserEntity.AccountStatus.ACTIVE);
        u.setBlocked(false);
        u.setSuspendedReason(null);
        u.setSuspendedUntil(null);
        userRepository.save(u);
        audit(actorId, u, "REACTIVATE", null, from, u.getStatus().name());
        return getDetail(playerId);
    }

    @Override
    @Transactional
    public PlayerAdminDetail ban(Long playerId, PlayerReasonBody body, Long actorId) {
        requireModerateHard(actorId);
        if (!StringUtils.hasText(body.getReason())) throw new BadRequestException("A reason is required to ban.");
        UserEntity u = requireUser(playerId);
        if (u.getStatus() != UserEntity.AccountStatus.ACTIVE && u.getStatus() != UserEntity.AccountStatus.SUSPENDED) {
            throw new ConflictException("Only an active or suspended player can be banned.");
        }
        String from = u.getStatus().name();
        u.setStatus(UserEntity.AccountStatus.BANNED);
        u.setBlocked(true);
        u.setSuspendedReason(body.getReason());
        u.setTokenVersion(u.getTokenVersion() + 1); // kick out any live sessions
        userRepository.save(u);
        audit(actorId, u, "BAN", body.getReason(), from, u.getStatus().name());
        return getDetail(playerId);
    }

    @Override
    @Transactional
    public PlayerAdminDetail unban(Long playerId, Long actorId) {
        requireModerateHard(actorId);
        UserEntity u = requireUser(playerId);
        if (u.getStatus() != UserEntity.AccountStatus.BANNED) {
            throw new ConflictException("Only a banned player can be unbanned.");
        }
        String from = u.getStatus().name();
        u.setStatus(UserEntity.AccountStatus.ACTIVE);
        u.setBlocked(false);
        u.setSuspendedReason(null);
        userRepository.save(u);
        audit(actorId, u, "UNBAN", null, from, u.getStatus().name());
        return getDetail(playerId);
    }

    @Override
    @Transactional
    public PlayerAdminDetail delete(Long playerId, PlayerReasonBody body, Long actorId) {
        requireModerateHard(actorId); // SUPER_ADMIN only
        if (body == null || !StringUtils.hasText(body.getReason())) {
            throw new BadRequestException("A reason is required to delete a player.");
        }
        UserEntity u = requireUser(playerId);
        if (u.getRole() != UserEntity.Role.PLAYER) {
            throw new ConflictException("This account is not a player.");
        }
        if (u.getStatus() == UserEntity.AccountStatus.DELETED) {
            throw new ConflictException("This player is already deleted.");
        }
        String from = u.getStatus().name();

        // Cancel the player's upcoming bookings (free the slot, notify the venue owner).
        int bookingsCancelled = 0;
        for (BookingEntity b : bookingRepository.findUpcomingForPlayer(u, UPCOMING_STATUSES, java.time.LocalDate.now())) {
            b.setStatus(BookingEntity.BookingStatus.CANCELLED);
            b.setCancellationReason(BookingEntity.CancellationReason.PLAYER);
            SlotEntity slot = b.getSlot();
            if (slot != null) {
                slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
                slotRepository.save(slot);
            }
            bookingRepository.save(b);
            if (b.getVenue() != null && b.getVenue().getOwner() != null) {
                notificationService.createNotification(
                        b.getVenue().getOwner(),
                        "Booking Cancelled",
                        String.format("A booking at %s on %s was cancelled because the player's account was closed.",
                                b.getVenue().getName(), b.getDate()),
                        NotificationEntity.NotificationType.BOOKING,
                        String.valueOf(b.getId()), "BOOKING");
            }
            bookingsCancelled++;
        }

        // Soft-delete: retain the row for history, free active_* for reuse, force-logout all sessions.
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
        u.setDeletedBookingsCancelled(bookingsCancelled);
        userRepository.save(u);

        audit(actorId, u, "DELETE", body.getReason(), from, u.getStatus().name());
        log.info("Player {} soft-deleted by admin {} — {} booking(s) cancelled.", playerId, actorId, bookingsCancelled);
        return getDetail(playerId);
    }

    @Override
    @Transactional
    public PlayerAdminDetail setVerification(Long playerId, PlayerVerificationBody body, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        UserEntity u = requireUser(playerId);
        String channel = body.getChannel() == null ? "" : body.getChannel().toUpperCase();
        boolean verified = Boolean.TRUE.equals(body.getVerified());
        switch (channel) {
            case "EMAIL" -> u.setEmailVerified(verified);
            case "PHONE" -> u.setPhoneVerified(verified);
            default -> throw new BadRequestException("channel must be EMAIL or PHONE");
        }
        userRepository.save(u);
        audit(actorId, u, verified ? "VERIFY" : "UNVERIFY", channel, null, null);
        return getDetail(playerId);
    }

    @Override
    @Transactional
    public void forceLogout(Long playerId, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        UserEntity u = requireUser(playerId);
        u.setTokenVersion(u.getTokenVersion() + 1);
        userRepository.save(u);
        audit(actorId, u, "FORCE_LOGOUT", null, null, null);
    }

    @Override
    @Transactional
    public void triggerPasswordReset(Long playerId, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        UserEntity u = requireUser(playerId);
        try {
            PasswordResetRequest req = new PasswordResetRequest();
            req.setEmail(u.getEmail());
            authService.requestPasswordReset(req); // dispatches OTP/link; returns no secret
        } catch (Exception e) {
            log.warn("Password reset dispatch failed for player {}: {}", playerId, e.getMessage());
        }
        audit(actorId, u, "RESET_PASSWORD", null, null, null);
    }

    @Override
    @Transactional
    public void message(Long playerId, PlayerMessageBody body, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        if (body.getChannels() == null || body.getChannels().isEmpty()) {
            throw new BadRequestException("At least one channel is required.");
        }
        if (!StringUtils.hasText(body.getBody())) throw new BadRequestException("Message body is required.");
        UserEntity u = requireUser(playerId);
        String title = StringUtils.hasText(body.getSubject()) ? body.getSubject() : "Message from Score-Adda";
        // IN_APP is delivered directly; EMAIL/SMS are best-effort (queued/logged) — no generic sender yet.
        if (body.getChannels().stream().anyMatch(c -> "IN_APP".equalsIgnoreCase(c))) {
            notificationService.createNotification(u, title, body.getBody(), NotificationEntity.NotificationType.SYSTEM);
        }
        audit(actorId, u, "MESSAGE", String.join(",", body.getChannels()), null, null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Sub-lists
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PlayerBookingPage getBookings(Long playerId, int page, int size) {
        UserEntity u = requireUser(playerId);
        Pageable pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 20 : size);
        Page<BookingEntity> p = bookingRepository.findByPlayerOrderByCreatedAtDesc(u, pageable);
        List<PlayerBookingRow> rows = p.getContent().stream().map(b -> new PlayerBookingRow()
                .bookingId(b.getId())
                .venueName(b.getVenue() != null ? b.getVenue().getName() : "—")
                .date(b.getDate())
                .slotLabel((b.getStartTime() != null ? b.getStartTime() : "") + "–" + (b.getEndTime() != null ? b.getEndTime() : ""))
                .amount(b.getAmount())
                .status(b.getStatus().name())).toList();
        return new PlayerBookingPage().content(rows)
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages()).size(p.getSize()).number(p.getNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerPaymentPage getPayments(Long playerId, int page, int size) {
        UserEntity u = requireUser(playerId);
        Pageable pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 20 : size);
        // Derive payments from bookings (no separate booking-payment entity; card data is never stored).
        Page<BookingEntity> p = bookingRepository.findByPlayerOrderByCreatedAtDesc(u, pageable);
        List<PlayerPaymentRow> rows = p.getContent().stream().map(b -> {
            boolean refunded = b.getPaymentStatus() == BookingEntity.PaymentStatus.REFUNDED;
            return new PlayerPaymentRow()
                    .paymentId(b.getId())
                    .date(odt(b.getCreatedAt()))
                    .amount(b.getAmount())
                    .methodLabel("Online")
                    .status(b.getPaymentStatus().name())
                    .refundedAmount(refunded ? b.getAmount() : null);
        }).toList();
        return new PlayerPaymentPage().content(rows)
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages()).size(p.getSize()).number(p.getNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerAuditPage getAudit(Long playerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 20 : size);
        Page<AdminAuditEntity> p = auditRepository.findByTargetId(playerId, pageable);
        List<PlayerAuditRow> rows = p.getContent().stream().map(a -> new PlayerAuditRow()
                .id(a.getId())
                .actorName(a.getActor() != null ? a.getActor().getName() : "System")
                .action(a.getAction())
                .reason(a.getReason())
                .fromStatus(a.getFromStatus())
                .toStatus(a.getToStatus())
                .createdAt(odt(a.getCreatedAt()))).toList();
        return new PlayerAuditPage().content(rows)
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages()).size(p.getSize()).number(p.getNumber());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private record Risk(String level, String reason) {}

    private Risk riskOf(long bookingCount, long playerCancels, long noShows, int disputeAtFaultCount) {
        int cancelRate = bookingCount > 0 ? (int) Math.round(100.0 * playerCancels / bookingCount) : 0;
        if (disputeAtFaultCount >= 3 || noShows >= RESTRICT_NOSHOW_HIGH || cancelRate >= 40) {
            String reason = disputeAtFaultCount >= 3 ? disputeAtFaultCount + " disputes lost"
                    : noShows >= RESTRICT_NOSHOW_HIGH ? noShows + " no-shows" : cancelRate + "% cancellation rate";
            return new Risk("HIGH", reason);
        }
        if (disputeAtFaultCount >= 1 || noShows >= 1 || cancelRate >= 20) {
            String reason = disputeAtFaultCount >= 1 ? disputeAtFaultCount + " dispute(s) lost"
                    : noShows >= 1 ? noShows + " no-show(s)" : cancelRate + "% cancellation rate";
            return new Risk("MEDIUM", reason);
        }
        return new Risk("NONE", null);
    }

    /** Map playerId → [lastActiveEpochSec, spend, bookingCount, playerCancels, noShows, lastActiveEpochSec]. */
    private Map<Long, long[]> aggregateFor(List<Long> ids) {
        Map<Long, long[]> out = new HashMap<>();
        if (ids == null || ids.isEmpty()) return out;
        for (Object[] r : bookingRepository.aggregateByPlayerIds(ids)) {
            Long id = ((Number) r[0]).longValue();
            long spend = r[1] != null ? ((Number) r[1]).longValue() : 0;
            long lastActive = 0;
            if (r[2] instanceof LocalDateTime ldt) lastActive = ldt.toEpochSecond(ZoneOffset.UTC);
            long count = r[3] != null ? ((Number) r[3]).longValue() : 0;
            long cancels = r[4] != null ? ((Number) r[4]).longValue() : 0;
            long noShows = r[5] != null ? ((Number) r[5]).longValue() : 0;
            out.put(id, new long[]{ lastActive, spend, count, cancels, noShows, lastActive });
        }
        return out;
    }

    private PlayerRow toRow(UserEntity u, long[] a) {
        long lastActive = a != null ? a[0] : 0;
        long spend = a != null ? a[1] : 0;
        long count = a != null ? a[2] : u.getTotalBookings();
        long cancels = a != null ? a[3] : 0;
        long noShows = a != null ? a[4] : 0;
        Risk risk = riskOf(count, cancels, noShows, u.getDisputeAtFaultCount());
        return new PlayerRow()
                .playerId(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .emailVerified(u.isEmailVerified())
                .phoneVerified(u.isPhoneVerified())
                .status(u.getStatus().name())
                .riskLevel(risk.level)
                .riskReason(risk.reason)
                .bookingCount((int) count)
                .totalSpend((int) spend)
                .lastActiveAt(lastActive > 0
                        ? OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(lastActive), ZoneOffset.UTC)
                        : odt(u.getLastActiveAt()))
                .joinedAt(odt(u.getCreatedAt()));
    }

    private List<String> availableActions(UserEntity.AccountStatus status) {
        // Status-based action set, then role-filtered for the caller (READ_ONLY → none;
        // SUPPORT → no BAN/UNBAN/DELETE; SUPER_ADMIN → all).
        List<String> base = switch (status) {
            case ACTIVE -> List.of("SUSPEND", "BAN", "VERIFY", "UNVERIFY", "FORCE_LOGOUT", "RESET_PASSWORD", "MESSAGE", "DELETE");
            case SUSPENDED -> List.of("REACTIVATE", "BAN", "MESSAGE", "DELETE");
            case BANNED -> List.of("UNBAN", "MESSAGE", "DELETE");
            case DELETED -> List.of();
        };
        return adminPermissionService.filterActions(base);
    }

    private void audit(Long actorId, UserEntity target, String action, String reason, String from, String to) {
        UserEntity actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
        auditRepository.save(AdminAuditEntity.builder()
                .actor(actor).target(target).action(action).reason(reason)
                .fromStatus(from).toStatus(to).build());
    }

    /** Ban/unban/delete are SUPER_ADMIN-only (delegates to the central RBAC service). */
    private void requireModerateHard(Long actorId) {
        adminPermissionService.requireModerateHard(actorId);
    }

    private UserEntity requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", id));
    }

    private OffsetDateTime odt(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }
}
