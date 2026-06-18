package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.DashboardBookingCountsDto;
import com.turfbook.backend.dto.DashboardEarningsDto;
import com.turfbook.backend.dto.DashboardStatsDto;
import com.turfbook.backend.dto.OwnerDashboardSummaryDto;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.PayoutEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.PayoutRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final PayoutRepository payoutRepository;

    @Override
    @Transactional(readOnly = true)
    public OwnerDashboardSummaryDto getSummary(Long ownerId) {
        log.info("OwnerDashboardService.getSummary() - ownerId={}", ownerId);

        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        // All date boundaries in Asia/Kolkata
        ZonedDateTime nowIst = ZonedDateTime.now(IST);
        LocalDate todayIst = nowIst.toLocalDate();

        // createdAt boundaries (mirrors existing getOwnerStats semantics for earnings)
        LocalDateTime todayStart  = todayIst.atStartOfDay();
        LocalDateTime todayEnd    = todayIst.plusDays(1).atStartOfDay();
        LocalDateTime weekStart   = todayIst.minusDays(7).atStartOfDay();
        LocalDateTime monthStart  = todayIst.withDayOfMonth(1).atStartOfDay();
        LocalDateTime last30Start = todayIst.minusDays(30).atStartOfDay();

        // ── Earnings (createdAt-based, paymentStatus=SUCCESS — mirrors existing /api/v1/owner/stats) ──
        long todayAmount        = bookingRepository.sumRevenueByOwnerAndDateRange(owner, todayStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);
        long weekAmount         = bookingRepository.sumRevenueByOwnerAndDateRange(owner, weekStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);
        long monthAmount        = bookingRepository.sumRevenueByOwnerAndDateRange(owner, monthStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);
        long pendingAmount      = payoutRepository.sumPendingByOwner(owner, PayoutEntity.PayoutStatus.PENDING);
        long todayBookingCount  = bookingRepository.countByOwnerAndDateRange(owner, todayStart, todayEnd);

        // ── Booking counts (slot date-based) ──
        long pendingRequests = bookingRepository.countByOwnerAndStatus(
                owner, BookingEntity.BookingStatus.PENDING);

        List<BookingEntity.BookingStatus> activeStatuses = List.of(
                BookingEntity.BookingStatus.PENDING,
                BookingEntity.BookingStatus.CONFIRMED,
                BookingEntity.BookingStatus.CHECKED_IN
        );
        long todayBookings = bookingRepository.countByOwnerAndDateAndStatusIn(owner, todayIst, activeStatuses);

        long upcoming = bookingRepository.countByOwnerAndDateAfterAndStatus(
                owner, todayIst, BookingEntity.BookingStatus.CONFIRMED);

        long completedLast30 = bookingRepository.countByOwnerAndStatusAndSlotDateBetween(
                owner, BookingEntity.BookingStatus.COMPLETED, todayIst.minusDays(30), todayIst);

        List<BookingEntity.BookingStatus> cancelStatuses = List.of(
                BookingEntity.BookingStatus.CANCELLED,
                BookingEntity.BookingStatus.REJECTED
        );
        long cancelledLast30 = bookingRepository.countByOwnerAndStatusInAndCreatedAtAfter(
                owner, cancelStatuses, last30Start);

        // ── Stats ──
        long usersConnected = bookingRepository.countDistinctPlayersByOwner(owner);
        long venueCount     = venueRepository.countByOwner(owner);
        long courtCount     = courtRepository.countByVenueOwner(owner);

        return OwnerDashboardSummaryDto.builder()
                .earnings(DashboardEarningsDto.builder()
                        .todayAmount(todayAmount)
                        .weekAmount(weekAmount)
                        .monthAmount(monthAmount)
                        .pendingAmount(pendingAmount)
                        .todayBookingCount(todayBookingCount)
                        .build())
                .bookings(DashboardBookingCountsDto.builder()
                        .requests(pendingRequests)
                        .today(todayBookings)
                        .upcoming(upcoming)
                        .completedLast30Days(completedLast30)
                        .cancelledLast30Days(cancelledLast30)
                        .build())
                .stats(DashboardStatsDto.builder()
                        .usersConnected(usersConnected)
                        .venueCount(venueCount)
                        .courtCount(courtCount)
                        .build())
                .build();
    }
}
