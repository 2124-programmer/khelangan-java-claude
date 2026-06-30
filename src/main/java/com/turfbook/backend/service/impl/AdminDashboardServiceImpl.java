package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.DisputeEntity;
import com.turfbook.backend.entity.CourtChangeRequestEntity;
import com.turfbook.backend.entity.SubscriptionChangeRequestEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.repository.*;
import com.turfbook.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    /** All period math, the greeting and dates are anchored to India time. */
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /**
     * MRR counts paying (ACTIVE) subscriptions only. Trials are live-gating but not yet revenue,
     * so they are excluded from MRR / active-subscription count and surfaced separately under
     * TRIALS_ENDING.
     */
    private static final List<SubscriptionStatus> PAYING_SUBS =
            List.of(SubscriptionStatus.ACTIVE);

    /** Venue statuses that count as work in the moderation queue. */
    private static final List<VenueEntity.VenueStatus> MODERATION_STATUSES =
            List.of(VenueEntity.VenueStatus.PENDING, VenueEntity.VenueStatus.CHANGES_REQUESTED);

    /** Open dispute work-queue (not yet RESOLVED/DISMISSED). */
    private static final List<DisputeEntity.DisputeStatus> OPEN_DISPUTE_STATUSES =
            List.of(DisputeEntity.DisputeStatus.OPEN,
                    DisputeEntity.DisputeStatus.UNDER_REVIEW,
                    DisputeEntity.DisputeStatus.NEEDS_INFO);

    private static final List<UserEntity.Role> SIGNUP_ROLES =
            List.of(UserEntity.Role.PLAYER, UserEntity.Role.OWNER);

    /** The assembled payload is heavy-ish and polled; cache it briefly per period. */
    private static final long CACHE_TTL_MILLIS = 45_000L;

    private record CacheEntry(DashboardSummary summary, long expiresAt) { }

    private final ConcurrentHashMap<DashboardPeriod, CacheEntry> cache = new ConcurrentHashMap<>();

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final DisputeRepository disputeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionChangeRequestRepository changeRequestRepository;
    private final CourtChangeRequestRepository courtChangeRequestRepository;
    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary getSummary(DashboardPeriod period) {
        DashboardPeriod p = period != null ? period : DashboardPeriod.TODAY;

        long nowMillis = System.currentTimeMillis();
        CacheEntry cached = cache.get(p);
        if (cached != null && cached.expiresAt() > nowMillis) {
            return cached.summary();
        }
        DashboardSummary fresh = build(p);
        cache.put(p, new CacheEntry(fresh, nowMillis + CACHE_TTL_MILLIS));
        return fresh;
    }

    private DashboardSummary build(DashboardPeriod p) {
        log.info("AdminDashboardService.getSummary() called - period={}", p);

        ZonedDateTime nowZ = ZonedDateTime.now(IST);
        LocalDate today = nowZ.toLocalDate();
        LocalDateTime now = nowZ.toLocalDateTime();
        Window window = windowFor(p, today);

        // ── Financial gate (flat ADMIN today = super-admin; SUPPORT role is future work) ──
        boolean canViewFinancials = canViewFinancials();

        DashboardSummary summary = new DashboardSummary();
        summary.setAsOf(nowZ.toOffsetDateTime());
        summary.setPeriod(p);
        summary.setCanViewFinancials(canViewFinancials);

        // ── Standing money: MRR + active subscriptions (no period, no historical trend) ──
        if (canViewFinancials) {
            MrrMetric mrr = new MrrMetric();
            mrr.setAmount(subscriptionRepository.sumPriceByStatusIn(PAYING_SUBS));
            mrr.setActiveSubscriptions(subscriptionRepository.countByStatusIn(PAYING_SUBS));
            mrr.setTrendPct(null);        // no month-over-month snapshot stored yet
            mrr.setTrendDirection(null);
            summary.setMrr(mrr);

            // GBV = booking volume (activity, NOT platform income) — period-bound, financial-only.
            long gbvCur = bookingRepository.sumRevenue(window.curStart, window.curEnd, BookingEntity.PaymentStatus.SUCCESS);
            long gbvPrev = bookingRepository.sumRevenue(window.prevStart, window.prevEnd, BookingEntity.PaymentStatus.SUCCESS);
            summary.setGbvThisPeriod(money(gbvCur, gbvPrev));

            // revenueThisPeriod intentionally omitted (subscriptions are activated offline / monthly;
            // a per-period collected figure would be misleading). MRR is the money hero.
            summary.setRevenueThisPeriod(null);
        }

        // ── Period-bound operational metrics (respond to the toggle) ──
        long bookingsCur = bookingRepository.countTodayBookings(window.curStart, window.curEnd);
        long bookingsPrev = bookingRepository.countTodayBookings(window.prevStart, window.prevEnd);
        summary.setBookingsThisPeriod(count(bookingsCur, bookingsPrev));

        long signupsCur = userRepository.countByRoleInAndCreatedAtBetween(SIGNUP_ROLES, window.curStart, window.curEnd);
        long signupsPrev = userRepository.countByRoleInAndCreatedAtBetween(SIGNUP_ROLES, window.prevStart, window.prevEnd);
        summary.setNewSignupsThisPeriod(count(signupsCur, signupsPrev));

        // ── Standing operational metrics (ignore the toggle — "right now") ──
        long activeVenues = venueRepository.countByStatus(VenueEntity.VenueStatus.LIVE);
        summary.setActiveVenues(count(activeVenues));

        long pendingModeration = venueRepository.countByStatusIn(MODERATION_STATUSES);
        summary.setPendingModeration(count(pendingModeration));

        // ── Needs-attention (standing work queues) ──
        long openDisputes = disputeRepository.countByStatusIn(OPEN_DISPUTE_STATUSES);
        long subRequests = changeRequestRepository.countByStatus(SubscriptionChangeRequestEntity.Status.PENDING);
        long courtChangeReqs = courtChangeRequestRepository.countByStatus(CourtChangeRequestEntity.Status.PENDING);
        long expiringSubs = subscriptionRepository.countByStatusInAndPeriodEndBetween(
                PAYING_SUBS, now, now.plusDays(7));
        long trialsEnding = subscriptionRepository.countByStatusAndTrialEndBetween(
                SubscriptionStatus.TRIALING, now, now.plusDays(3));

        List<NeedsAttentionItem> attention = new ArrayList<>();
        attention.add(attentionItem(NeedsAttentionKey.PENDING_APPROVALS, "Pending approvals",
                pendingModeration, "Venues", Map.of("tab", "PENDING")));
        attention.add(attentionItem(NeedsAttentionKey.SUBSCRIPTION_REQUESTS, "Subscription requests",
                subRequests, "SubscriptionManagement", Map.of("tab", "requests")));
        // Court-change requests are actioned only by super-admins; the FE hides this tile for other
        // admin roles (the shared 45s dashboard cache is role-agnostic, so we always include it here).
        attention.add(attentionItem(NeedsAttentionKey.COURT_CHANGE_REQUESTS, "Court change requests",
                courtChangeReqs, "CourtChangeRequests", null));
        attention.add(attentionItem(NeedsAttentionKey.OPEN_DISPUTES, "Open disputes",
                openDisputes, "Disputes", null));
        attention.add(attentionItem(NeedsAttentionKey.EXPIRING_SUBSCRIPTIONS, "Expiring subscriptions",
                expiringSubs, "SubscriptionManagement", Map.of("tab", "active")));
        attention.add(attentionItem(NeedsAttentionKey.TRIALS_ENDING, "Trials ending",
                trialsEnding, "SubscriptionManagement", Map.of("tab", "active")));
        summary.setNeedsAttention(attention);

        // ── Management tile counts (standing) ──
        ManagementCounts counts = new ManagementCounts();
        counts.setVenues(venueRepository.count());
        counts.setPlayers(userRepository.countByRole(UserEntity.Role.PLAYER));
        counts.setOwners(userRepository.countByRole(UserEntity.Role.OWNER));
        counts.setBookings(bookingRepository.count());
        counts.setOpenDisputes(openDisputes);
        counts.setActiveCoupons(couponRepository.countActiveCoupons(today));
        summary.setCounts(counts);

        log.info("AdminDashboardService.getSummary() completed - period={}, bookings={}, signups={}, openDisputes={}",
                p, bookingsCur, signupsCur, openDisputes);
        return summary;
    }

    /**
     * Today = full ADMIN access. There is no SUPPORT/READ_ONLY split yet (the role model is a single
     * flat ADMIN), so super-admin sees money. When sub-roles land, derive this from the caller's role.
     */
    private boolean canViewFinancials() {
        return true;
    }

    // ── Period windows (current + the immediately preceding same-length period) ──

    private record Window(LocalDateTime curStart, LocalDateTime curEnd,
                          LocalDateTime prevStart, LocalDateTime prevEnd) { }

    private Window windowFor(DashboardPeriod period, LocalDate today) {
        LocalDateTime curStart;
        LocalDateTime curEnd;
        LocalDateTime prevStart;
        switch (period) {
            case WEEK -> {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                curStart = monday.atStartOfDay();
                curEnd = monday.plusWeeks(1).atStartOfDay();
                prevStart = monday.minusWeeks(1).atStartOfDay();
            }
            case MONTH -> {
                LocalDate first = today.withDayOfMonth(1);
                curStart = first.atStartOfDay();
                curEnd = first.plusMonths(1).atStartOfDay();
                prevStart = first.minusMonths(1).atStartOfDay();
            }
            default -> { // TODAY
                curStart = today.atStartOfDay();
                curEnd = today.plusDays(1).atStartOfDay();
                prevStart = today.minusDays(1).atStartOfDay();
            }
        }
        return new Window(curStart, curEnd, prevStart, curStart);
    }

    // ── Metric builders ──

    /** Standing count metric — no comparison period, so no trend. */
    private CountMetric count(long value) {
        CountMetric m = new CountMetric();
        m.setValue(value);
        m.setTrendPct(null);
        m.setTrendDirection(null);
        return m;
    }

    private CountMetric count(long current, long previous) {
        CountMetric m = new CountMetric();
        m.setValue(current);
        applyTrend(current, previous, m::setTrendPct, m::setTrendDirection);
        return m;
    }

    private MoneyMetric money(long current, long previous) {
        MoneyMetric m = new MoneyMetric();
        m.setAmount(current);
        applyTrend(current, previous, m::setTrendPct, m::setTrendDirection);
        return m;
    }

    /**
     * Trend = round((current − previous) / previous * 100). When previous is 0 we leave both null
     * (no divide-by-zero; the client renders "new" / no chip).
     */
    private void applyTrend(long current, long previous,
                            java.util.function.Consumer<BigDecimal> setPct,
                            java.util.function.Consumer<TrendDirection> setDir) {
        if (previous <= 0) {
            setPct.accept(null);
            setDir.accept(null);
            return;
        }
        BigDecimal pct = BigDecimal.valueOf((current - previous) * 100.0 / previous)
                .setScale(0, RoundingMode.HALF_UP);
        setPct.accept(pct);
        int cmp = pct.signum();
        setDir.accept(cmp > 0 ? TrendDirection.UP : cmp < 0 ? TrendDirection.DOWN : TrendDirection.FLAT);
    }

    private NeedsAttentionItem attentionItem(NeedsAttentionKey key, String label, long count,
                                             String screen, Map<String, String> params) {
        NeedsAttentionItem item = new NeedsAttentionItem();
        item.setKey(key);
        item.setLabel(label);
        item.setCount(count);
        item.setTone(count > 0 ? DashboardTone.DANGER : DashboardTone.NEUTRAL);
        item.setDeepLinkScreen(screen);
        item.setDeepLinkParams(params);
        return item;
    }
}
