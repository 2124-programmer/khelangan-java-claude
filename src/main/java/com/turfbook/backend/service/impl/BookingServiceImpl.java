package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.dto.BookingPage;
import com.turfbook.backend.dto.BulkCreateBookingRequest;
import com.turfbook.backend.dto.CreateBookingRequest;
import com.turfbook.backend.entity.*;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.BookingMapper;
import com.turfbook.backend.repository.*;
import com.turfbook.backend.service.BookingService;
import com.turfbook.backend.service.NotificationService;
import com.turfbook.backend.service.OwnerSettingsService;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final PlatformSettingsRepository settingsRepository;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;
    private final OwnerSettingsService ownerSettingsService;
    private final SubscriptionGate subscriptionGate;

    // ─── listBookings ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BookingPage listBookings(UserEntity currentUser, String status,
                                    java.time.LocalDate date, java.time.LocalDate dateFrom,
                                    int page, int size) {
        log.info("BookingService.listBookings() called - userId={}, role={}, status={}, date={}, dateFrom={}",
                currentUser.getId(), currentUser.getRole(), status, date, dateFrom);
        Pageable pageable = PageRequest.of(page, size);
        BookingEntity.BookingStatus bookingStatus = parseStatus(status);

        Page<BookingEntity> entityPage;

        // "Completed" tab must also surface legacy CHECKED_IN rows
        final List<BookingEntity.BookingStatus> completedStatuses =
                List.of(BookingEntity.BookingStatus.COMPLETED, BookingEntity.BookingStatus.CHECKED_IN);
        final boolean isCompletedQuery = "COMPLETED".equalsIgnoreCase(status);

        // "Cancelled" tab surfaces CANCELLED + REJECTED + EXPIRED for players
        final List<BookingEntity.BookingStatus> cancelledStatuses =
                List.of(BookingEntity.BookingStatus.CANCELLED, BookingEntity.BookingStatus.REJECTED, BookingEntity.BookingStatus.EXPIRED);
        final boolean isCancelledQuery = "CANCELLED".equalsIgnoreCase(status);

        switch (currentUser.getRole()) {
            case PLAYER -> {
                if (isCompletedQuery) {
                    entityPage = bookingRepository.findByPlayerAndStatusInOrderByCreatedAtDesc(
                            currentUser, completedStatuses, pageable);
                } else if (isCancelledQuery) {
                    entityPage = bookingRepository.findByPlayerAndStatusInOrderByCreatedAtDesc(
                            currentUser, cancelledStatuses, pageable);
                } else if (bookingStatus != null) {
                    entityPage = bookingRepository.findByPlayerAndStatusOrderByCreatedAtDesc(
                            currentUser, bookingStatus, pageable);
                } else {
                    entityPage = bookingRepository.findByPlayerOrderByCreatedAtDesc(currentUser, pageable);
                }
            }
            case OWNER -> {
                if (isCompletedQuery) {
                    entityPage = bookingRepository.findByVenueOwnerAndStatusIn(
                            currentUser, completedStatuses, pageable);
                } else if (date != null || dateFrom != null) {
                    entityPage = bookingRepository.findByVenueOwnerWithDateFilter(
                            currentUser, bookingStatus, date, dateFrom, pageable);
                } else {
                    entityPage = bookingRepository.findByVenueOwner(currentUser, bookingStatus, pageable);
                }
            }
            default -> entityPage = bookingRepository.findAllByStatus(bookingStatus, pageable);
        }

        return toBookingPage(entityPage);
    }

    // ─── createBooking ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingDto createBooking(Long playerId, CreateBookingRequest request) {
        log.info("BookingService.createBooking() called - playerId={}, venueId={}, date={}, startTime={}",
                playerId, request.getVenueId(), request.getDate(), request.getStartTime());

        // 1. Load player
        UserEntity player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", playerId));

        // 2. Extract date and time from request
        LocalDate date      = request.getDate();
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime   = LocalTime.parse(request.getEndTime());

        // 3. Load venue and court
        VenueEntity venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.getVenueId()));

        // Block NEW bookings when the venue is not live (unapproved or no active subscription /
        // expired). Already-confirmed future bookings are never affected by this gate.
        if (!subscriptionGate.isVenueLive(venue.getId())) {
            throw new ConflictException("This venue is not currently accepting new bookings.");
        }

        CourtEntity court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", request.getCourtId()));

        if (!court.getVenue().getId().equals(venue.getId())) {
            throw new IllegalArgumentException("Court does not belong to the specified venue");
        }

        // 4. Find existing slot (BLOCKED / old-AVAILABLE from prior cancellations) or null for fresh hours
        SlotEntity slot = slotRepository.findByCourtAndDateAndStartTime(court, date, startTime)
                .orElse(null);

        if (slot != null && slot.getStatus() == SlotEntity.SlotStatus.BLOCKED) {
            throw new ConflictException("Slot is blocked by the owner");
        }

        // 5. Double-booking guard via active bookings
        boolean alreadyBooked = bookingRepository
                .findByCourtAndDateAndStatusIn(court, date,
                        List.of(BookingEntity.BookingStatus.CONFIRMED, BookingEntity.BookingStatus.PENDING))
                .stream().anyMatch(b -> b.getStartTime().equals(startTime));
        if (alreadyBooked) {
            throw new ConflictException("Slot is not available for booking");
        }

        // 6. Platform settings
        PlatformSettingsEntity settings = settingsRepository.findById(1L)
                .orElseGet(() -> PlatformSettingsEntity.builder()
                        .id(1L)
                        .commissionPercent(10)
                        .convenienceFee(20)
                        .build());

        int slotPrice = slot != null ? slot.getPrice() : court.effectivePricePerHour();
        int convenienceFee = settings.getConvenienceFee();
        int discount = 0;
        String couponCode = null;

        // 5. Coupon
        if (StringUtils.hasText(request.getCouponCode())) {
            var couponOpt = couponRepository.findByCode(request.getCouponCode().trim().toUpperCase());
            if (couponOpt.isPresent()) {
                CouponEntity coupon = couponOpt.get();
                int baseAmount = slotPrice + convenienceFee;

                if (coupon.isActive()
                        && !coupon.getValidUntil().isBefore(java.time.LocalDate.now())
                        && coupon.getUsedCount() < coupon.getMaxUses()
                        && baseAmount >= coupon.getMinBooking()) {

                    if (coupon.getDiscountType() == CouponEntity.DiscountType.PERCENT) {
                        discount = (baseAmount * coupon.getDiscountValue()) / 100;
                        if (coupon.getMaxDiscount() != null && discount > coupon.getMaxDiscount()) {
                            discount = coupon.getMaxDiscount();
                        }
                    } else {
                        discount = coupon.getDiscountValue();
                        if (discount > baseAmount) discount = baseAmount;
                    }

                    coupon.setUsedCount(coupon.getUsedCount() + 1);
                    couponRepository.save(coupon);
                    couponCode = coupon.getCode();
                }
            }
        }

        // 6. Totals
        int effectiveAmount = slotPrice + convenienceFee - discount;
        int commission = (effectiveAmount * settings.getCommissionPercent()) / 100;

        // 7. Determine initial status and create/update slot (never persisted as AVAILABLE)
        UserEntity owner = venue.getOwner();
        // Auto-accept applies only when the owner enabled it AND this venue's active plan
        // grants the AUTO_ACCEPT feature; otherwise the booking stays manual (PENDING).
        boolean autoAccept = ownerSettingsService.isAutoAccept(owner.getId())
                && subscriptionGate.hasFeature(venue.getId(), FeatureCode.AUTO_ACCEPT);
        SlotEntity.SlotStatus targetStatus = autoAccept ? SlotEntity.SlotStatus.BOOKED : SlotEntity.SlotStatus.HELD;

        if (slot == null) {
            slot = SlotEntity.builder()
                    .court(court)
                    .date(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .price(slotPrice)
                    .status(targetStatus)
                    .build();
        } else {
            slot.setStatus(targetStatus);
        }
        slot = slotRepository.save(slot);

        // 8. Create booking
        BookingEntity booking = BookingEntity.builder()
                .player(player)
                .venue(venue)
                .court(court)
                .slot(slot)
                .sport(request.getSport())
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .amount(effectiveAmount)
                .convenienceFee(convenienceFee)
                .discount(discount)
                .commission(commission)
                .status(autoAccept ? BookingEntity.BookingStatus.CONFIRMED : BookingEntity.BookingStatus.PENDING)
                .paymentStatus(autoAccept ? BookingEntity.PaymentStatus.SUCCESS : BookingEntity.PaymentStatus.PENDING)
                .couponCode(couponCode)
                .hasReview(false)
                .build();

        booking = bookingRepository.save(booking);

        // 9. Increment totalBookings only on immediate confirmation
        if (autoAccept) {
            player.setTotalBookings(player.getTotalBookings() + 1);
            userRepository.save(player);
        }

        // 10. Notifications
        String bookingRef = String.valueOf(booking.getId());
        if (autoAccept) {
            notificationService.createNotification(
                    player,
                    "Booking Confirmed",
                    String.format("Your booking at %s on %s (%s – %s) is confirmed. Amount paid: ₹%d",
                            venue.getName(), slot.getDate(), slot.getStartTime(), slot.getEndTime(), effectiveAmount),
                    NotificationEntity.NotificationType.BOOKING
            );
            notificationService.createNotification(
                    owner,
                    "New Booking Received",
                    String.format("New booking by %s at %s on %s. Amount: ₹%d",
                            player.getName(), venue.getName(), slot.getDate(), effectiveAmount),
                    NotificationEntity.NotificationType.BOOKING, bookingRef, "BOOKING"
            );
        } else {
            notificationService.createNotification(
                    player,
                    "Booking Request Sent",
                    String.format("Your booking request at %s on %s (%s – %s) has been sent. Awaiting owner approval.",
                            venue.getName(), slot.getDate(), slot.getStartTime(), slot.getEndTime()),
                    NotificationEntity.NotificationType.BOOKING
            );
            notificationService.createNotification(
                    owner,
                    "New Booking Request",
                    String.format("%s has requested to book %s at %s on %s. Please accept or reject.",
                            player.getName(), court.getName(), venue.getName(), slot.getDate()),
                    NotificationEntity.NotificationType.BOOKING, bookingRef, "BOOKING"
            );
        }

        log.info("Booking created: id={}, player={}, venue={}, status={}, amount={}",
                booking.getId(), playerId, venue.getId(), booking.getStatus(), effectiveAmount);

        return bookingMapper.toDto(booking);
    }

    // ─── getBooking ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BookingDto getBooking(Long id, UserEntity currentUser) {
        log.info("BookingService.getBooking() called - id={}, userId={}", id, currentUser.getId());
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));

        if (currentUser.getRole() == UserEntity.Role.PLAYER
                && !booking.getPlayer().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this booking");
        }
        if (currentUser.getRole() == UserEntity.Role.OWNER
                && !booking.getVenue().getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this booking");
        }

        return bookingMapper.toDto(booking);
    }

    // ─── cancelBooking ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingDto cancelBooking(Long id, Long playerId) {
        log.info("BookingService.cancelBooking() called - id={}, playerId={}", id, playerId);
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));

        if (!booking.getPlayer().getId().equals(playerId)) {
            throw new UnauthorizedException("You can only cancel your own bookings");
        }

        if (booking.getStatus() != BookingEntity.BookingStatus.CONFIRMED
                && booking.getStatus() != BookingEntity.BookingStatus.PENDING) {
            throw new ConflictException(
                    "Cannot cancel booking with status: " + booking.getStatus() +
                    ". Only CONFIRMED or PENDING bookings can be cancelled.");
        }

        // PENDING cancellation: no payment was taken, so no refund calculation needed
        if (booking.getStatus() == BookingEntity.BookingStatus.PENDING) {
            booking.setPaymentStatus(BookingEntity.PaymentStatus.PENDING);
        } else {
            // Refund logic based on hours until slot
            LocalDateTime slotDateTime = LocalDateTime.of(booking.getDate(), booking.getStartTime());
            long hoursUntilSlot = ChronoUnit.HOURS.between(LocalDateTime.now(), slotDateTime);
            if (hoursUntilSlot >= 12) {
                booking.setPaymentStatus(BookingEntity.PaymentStatus.REFUNDED);
                log.info("Booking {} cancelled with REFUND ({}h until slot)", id, hoursUntilSlot);
            } else {
                log.info("Booking {} cancelled with NO REFUND ({}h until slot)", id, hoursUntilSlot);
            }
        }

        booking.setStatus(BookingEntity.BookingStatus.CANCELLED);
        booking.setCancellationReason(BookingEntity.CancellationReason.PLAYER);

        SlotEntity slot = booking.getSlot();
        slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
        slotRepository.save(slot);

        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getPlayer(),
                "Booking Cancelled",
                String.format("Your booking at %s on %s (%s – %s) has been cancelled.%s",
                        booking.getVenue().getName(),
                        booking.getDate(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        booking.getPaymentStatus() == BookingEntity.PaymentStatus.REFUNDED
                                ? " A refund has been initiated." : ""),
                NotificationEntity.NotificationType.BOOKING
        );

        // Dismiss the owner's actionable "New Booking Request" notification so that
        // Accept/Reject buttons no longer appear for a booking that is already cancelled.
        notificationService.dismissNotificationsForBooking(
                booking.getVenue().getOwner(),
                String.valueOf(booking.getId())
        );

        notificationService.createNotification(
                booking.getVenue().getOwner(),
                "Booking Cancelled by Player",
                String.format("%s has cancelled their booking at %s on %s (%s – %s).",
                        booking.getPlayer().getName(),
                        booking.getVenue().getName(),
                        booking.getDate(),
                        slot.getStartTime(),
                        slot.getEndTime()),
                NotificationEntity.NotificationType.BOOKING,
                String.valueOf(booking.getId()),
                "BOOKING"
        );

        return bookingMapper.toDto(booking);
    }

    // ─── cancelBookingGroup ────────────────────────────────────────────────

    @Override
    @Transactional
    public List<BookingDto> cancelBookingGroup(String groupId, Long playerId) {
        log.info("BookingService.cancelBookingGroup() called - groupId={}, playerId={}", groupId, playerId);
        List<BookingEntity> bookings = bookingRepository.findByGroupIdOrderByStartTimeAsc(groupId);
        if (bookings.isEmpty()) {
            throw new ResourceNotFoundException("BookingGroup", "groupId", groupId);
        }

        UserEntity player = bookings.get(0).getPlayer();
        if (!player.getId().equals(playerId)) {
            throw new UnauthorizedException("You can only cancel your own bookings");
        }

        VenueEntity venue = bookings.get(0).getVenue();
        LocalDate date = bookings.get(0).getDate();
        List<BookingDto> results = new ArrayList<>();

        for (BookingEntity booking : bookings) {
            if (booking.getStatus() != BookingEntity.BookingStatus.CONFIRMED
                    && booking.getStatus() != BookingEntity.BookingStatus.PENDING) continue;

            if (booking.getStatus() == BookingEntity.BookingStatus.PENDING) {
                booking.setPaymentStatus(BookingEntity.PaymentStatus.PENDING);
            } else {
                LocalDateTime slotDateTime = LocalDateTime.of(booking.getDate(), booking.getStartTime());
                long hoursUntilSlot = ChronoUnit.HOURS.between(LocalDateTime.now(), slotDateTime);
                booking.setPaymentStatus(hoursUntilSlot >= 12
                        ? BookingEntity.PaymentStatus.REFUNDED
                        : BookingEntity.PaymentStatus.PENDING);
            }

            booking.setStatus(BookingEntity.BookingStatus.CANCELLED);
            booking.setCancellationReason(BookingEntity.CancellationReason.PLAYER);
            SlotEntity slot = booking.getSlot();
            if (slot != null) {
                slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
                slotRepository.save(slot);
            }
            results.add(bookingMapper.toDto(bookingRepository.save(booking)));
        }

        if (results.isEmpty()) {
            throw new ConflictException("No cancellable bookings found in group");
        }

        String slotSummary = results.stream()
                .map(r -> r.getStartTime() + "–" + r.getEndTime())
                .reduce((a, b) -> a + ", " + b).orElse("");

        boolean anyRefund = results.stream()
                .anyMatch(r -> "REFUNDED".equals(r.getPaymentStatus()));

        notificationService.createNotification(
                player,
                "Bookings Cancelled",
                String.format("Your %d-slot booking at %s on %s (%s) has been cancelled.%s",
                        results.size(), venue.getName(), date, slotSummary,
                        anyRefund ? " A refund has been initiated." : ""),
                NotificationEntity.NotificationType.BOOKING
        );

        // Dismiss the owner's group "New Booking Request" notification
        notificationService.dismissNotificationsForBooking(venue.getOwner(), groupId);

        notificationService.createNotification(
                venue.getOwner(),
                "Booking Cancelled by Player",
                String.format("%s has cancelled their %d-slot booking at %s on %s (%s).",
                        player.getName(), results.size(), venue.getName(), date, slotSummary),
                NotificationEntity.NotificationType.BOOKING,
                groupId,
                "BOOKING_GROUP"
        );

        log.info("BookingGroup {} cancelled by player {}: {} bookings cancelled", groupId, playerId, results.size());
        return results;
    }

    // ─── acceptBooking ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingDto acceptBooking(Long bookingId, Long ownerId) {
        log.info("BookingService.acceptBooking() called - bookingId={}, ownerId={}", bookingId, ownerId);
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingEntity.BookingStatus.PENDING) {
            throw new ConflictException("Only PENDING bookings can be accepted. Current status: " + booking.getStatus());
        }

        if (!booking.getVenue().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        booking.setStatus(BookingEntity.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(BookingEntity.PaymentStatus.SUCCESS);

        SlotEntity slot = booking.getSlot();
        slot.setStatus(SlotEntity.SlotStatus.BOOKED);
        slotRepository.save(slot);

        UserEntity player = booking.getPlayer();
        player.setTotalBookings(player.getTotalBookings() + 1);
        userRepository.save(player);

        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                player,
                "Booking Confirmed",
                String.format("Your booking request at %s on %s (%s – %s) has been accepted. Amount: ₹%d",
                        booking.getVenue().getName(), booking.getDate(),
                        booking.getStartTime(), booking.getEndTime(), booking.getAmount()),
                NotificationEntity.NotificationType.BOOKING
        );

        log.info("Booking {} accepted by owner {}", bookingId, ownerId);
        return bookingMapper.toDto(booking);
    }

    // ─── rejectBooking ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingDto rejectBooking(Long bookingId, Long ownerId) {
        log.info("BookingService.rejectBooking() called - bookingId={}, ownerId={}", bookingId, ownerId);
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingEntity.BookingStatus.PENDING) {
            throw new ConflictException("Only PENDING bookings can be rejected. Current status: " + booking.getStatus());
        }

        if (!booking.getVenue().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        booking.setStatus(BookingEntity.BookingStatus.REJECTED);
        booking.setPaymentStatus(BookingEntity.PaymentStatus.FAILED);
        booking.setCancellationReason(BookingEntity.CancellationReason.OWNER);

        SlotEntity slot = booking.getSlot();
        slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
        slotRepository.save(slot);

        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getPlayer(),
                "Booking Request Rejected",
                String.format("Your booking request at %s on %s was not accepted by the owner.",
                        booking.getVenue().getName(), booking.getDate()),
                NotificationEntity.NotificationType.BOOKING
        );

        log.info("Booking {} rejected by owner {}", bookingId, ownerId);
        return bookingMapper.toDto(booking);
    }

    // ─── bulkCreateBookings ────────────────────────────────────────────────

    @Override
    @Transactional
    public List<BookingDto> bulkCreateBookings(Long playerId, BulkCreateBookingRequest request) {
        log.info("BookingService.bulkCreateBookings() called - playerId={}, courtId={}, date={}, count={}",
                playerId, request.getCourtId(), request.getDate(),
                request.getStartTimes() == null ? 0 : request.getStartTimes().size());

        if (request.getStartTimes() == null || request.getStartTimes().isEmpty()) {
            throw new IllegalArgumentException("startTimes must not be empty");
        }

        UserEntity player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", playerId));

        VenueEntity venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.getVenueId()));

        // Block NEW bookings when the venue is not live (unapproved or no active subscription /
        // expired). Already-confirmed future bookings are never affected by this gate.
        if (!subscriptionGate.isVenueLive(venue.getId())) {
            throw new ConflictException("This venue is not currently accepting new bookings.");
        }

        CourtEntity court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", request.getCourtId()));

        if (!court.getVenue().getId().equals(venue.getId())) {
            throw new IllegalArgumentException("Court does not belong to the specified venue");
        }

        PlatformSettingsEntity settings = settingsRepository.findById(1L)
                .orElseGet(() -> PlatformSettingsEntity.builder()
                        .id(1L).commissionPercent(10).convenienceFee(20).build());

        UserEntity owner = venue.getOwner();
        // Auto-accept applies only when the owner enabled it AND this venue's active plan
        // grants the AUTO_ACCEPT feature; otherwise the booking stays manual (PENDING).
        boolean autoAccept = ownerSettingsService.isAutoAccept(owner.getId())
                && subscriptionGate.hasFeature(venue.getId(), FeatureCode.AUTO_ACCEPT);
        SlotEntity.SlotStatus targetStatus = autoAccept
                ? SlotEntity.SlotStatus.BOOKED : SlotEntity.SlotStatus.HELD;
        LocalDate date = request.getDate();
        String groupId = UUID.randomUUID().toString();

        List<BookingDto> results = new ArrayList<>();

        // Pre-load active bookings for this court+date once to avoid N+1 queries
        List<BookingEntity> activeBookings = bookingRepository.findByCourtAndDateAndStatusIn(
                court, date,
                List.of(BookingEntity.BookingStatus.CONFIRMED, BookingEntity.BookingStatus.PENDING));

        for (String startTimeStr : request.getStartTimes()) {
            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = startTime.plusHours(1);

            SlotEntity slot = slotRepository.findByCourtAndDateAndStartTime(court, date, startTime)
                    .orElse(null);

            if (slot != null && slot.getStatus() == SlotEntity.SlotStatus.BLOCKED) {
                throw new ConflictException("Slot " + startTimeStr + " is blocked by the owner");
            }

            boolean alreadyBooked = activeBookings.stream()
                    .anyMatch(b -> b.getStartTime().equals(startTime));
            if (alreadyBooked) {
                throw new ConflictException("Slot " + startTimeStr + " is not available for booking");
            }

            int slotPrice = slot != null ? slot.getPrice() : court.effectivePricePerHour();
            int convenienceFee = settings.getConvenienceFee();
            int effectiveAmount = slotPrice + convenienceFee;
            int commission = (effectiveAmount * settings.getCommissionPercent()) / 100;

            if (slot == null) {
                slot = SlotEntity.builder()
                        .court(court).date(date).startTime(startTime).endTime(endTime)
                        .price(slotPrice).status(targetStatus).build();
            } else {
                slot.setStatus(targetStatus);
            }
            slot = slotRepository.save(slot);

            BookingEntity booking = BookingEntity.builder()
                    .player(player).venue(venue).court(court).slot(slot)
                    .sport(request.getSport())
                    .date(date).startTime(startTime).endTime(endTime)
                    .amount(effectiveAmount).convenienceFee(convenienceFee)
                    .discount(0).commission(commission)
                    .status(autoAccept ? BookingEntity.BookingStatus.CONFIRMED : BookingEntity.BookingStatus.PENDING)
                    .paymentStatus(autoAccept ? BookingEntity.PaymentStatus.SUCCESS : BookingEntity.PaymentStatus.PENDING)
                    .hasReview(false)
                    .groupId(groupId)
                    .build();

            results.add(bookingMapper.toDto(bookingRepository.save(booking)));
        }

        String slotSummary = request.getStartTimes().stream()
                .map(t -> t + "–" + LocalTime.parse(t).plusHours(1).toString().substring(0, 5))
                .reduce((a, b) -> a + ", " + b).orElse("");
        int totalAmount = results.stream().mapToInt(BookingDto::getAmount).sum();

        if (autoAccept) {
            player.setTotalBookings(player.getTotalBookings() + results.size());
            userRepository.save(player);

            notificationService.createNotification(player, "Bookings Confirmed",
                    String.format("%d slots confirmed at %s on %s (%s). Total: ₹%d",
                            results.size(), venue.getName(), date, slotSummary, totalAmount),
                    NotificationEntity.NotificationType.BOOKING);
            notificationService.createNotification(owner, "New Bookings Received",
                    String.format("%s booked %d slots at %s on %s (%s). Total: ₹%d",
                            player.getName(), results.size(), venue.getName(), date, slotSummary, totalAmount),
                    NotificationEntity.NotificationType.BOOKING, groupId, "BOOKING_GROUP");
        } else {
            notificationService.createNotification(player, "Booking Requests Sent",
                    String.format("%d slot requests sent to %s on %s (%s). Awaiting confirmation.",
                            results.size(), venue.getName(), date, slotSummary),
                    NotificationEntity.NotificationType.BOOKING);
            notificationService.createNotification(owner, "New Booking Request",
                    String.format("%s requested %d slots at %s on %s (%s). Please accept or reject.",
                            player.getName(), results.size(), venue.getName(), date, slotSummary),
                    NotificationEntity.NotificationType.BOOKING, groupId, "BOOKING_GROUP");
        }

        log.info("Bulk booking created: {} bookings, player={}, venue={}", results.size(), playerId, venue.getId());
        return results;
    }

    // ─── acceptBookingGroup ────────────────────────────────────────────────

    @Override
    @Transactional
    public List<BookingDto> acceptBookingGroup(String groupId, Long ownerId) {
        log.info("BookingService.acceptBookingGroup() called - groupId={}, ownerId={}", groupId, ownerId);
        List<BookingEntity> bookings = bookingRepository.findByGroupIdOrderByStartTimeAsc(groupId);
        if (bookings.isEmpty()) {
            throw new ResourceNotFoundException("BookingGroup", "groupId", groupId);
        }

        VenueEntity venue = bookings.get(0).getVenue();
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        List<BookingDto> results = new ArrayList<>();
        UserEntity player = bookings.get(0).getPlayer();

        for (BookingEntity booking : bookings) {
            if (booking.getStatus() != BookingEntity.BookingStatus.PENDING) continue;
            booking.setStatus(BookingEntity.BookingStatus.CONFIRMED);
            booking.setPaymentStatus(BookingEntity.PaymentStatus.SUCCESS);
            SlotEntity slot = booking.getSlot();
            if (slot != null) {
                slot.setStatus(SlotEntity.SlotStatus.BOOKED);
                slotRepository.save(slot);
            }
            player.setTotalBookings(player.getTotalBookings() + 1);
            results.add(bookingMapper.toDto(bookingRepository.save(booking)));
        }
        userRepository.save(player);

        int totalAmount = results.stream().mapToInt(BookingDto::getAmount).sum();
        notificationService.createNotification(
                player,
                "Booking Confirmed",
                String.format("%d slots confirmed at %s on %s. Total: ₹%d",
                        results.size(), venue.getName(), bookings.get(0).getDate(), totalAmount),
                NotificationEntity.NotificationType.BOOKING
        );

        log.info("BookingGroup {} accepted by owner {}: {} bookings confirmed", groupId, ownerId, results.size());
        return results;
    }

    // ─── rejectBookingGroup ────────────────────────────────────────────────

    @Override
    @Transactional
    public List<BookingDto> rejectBookingGroup(String groupId, Long ownerId) {
        log.info("BookingService.rejectBookingGroup() called - groupId={}, ownerId={}", groupId, ownerId);
        List<BookingEntity> bookings = bookingRepository.findByGroupIdOrderByStartTimeAsc(groupId);
        if (bookings.isEmpty()) {
            throw new ResourceNotFoundException("BookingGroup", "groupId", groupId);
        }

        VenueEntity venue = bookings.get(0).getVenue();
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        List<BookingDto> results = new ArrayList<>();
        UserEntity player = bookings.get(0).getPlayer();

        for (BookingEntity booking : bookings) {
            if (booking.getStatus() != BookingEntity.BookingStatus.PENDING) continue;
            booking.setStatus(BookingEntity.BookingStatus.REJECTED);
            booking.setPaymentStatus(BookingEntity.PaymentStatus.FAILED);
            booking.setCancellationReason(BookingEntity.CancellationReason.OWNER);
            SlotEntity slot = booking.getSlot();
            if (slot != null) {
                slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
                slotRepository.save(slot);
            }
            results.add(bookingMapper.toDto(bookingRepository.save(booking)));
        }

        notificationService.createNotification(
                player,
                "Booking Request Rejected",
                String.format("Your %d-slot booking request at %s on %s was not accepted by the owner.",
                        results.size(), venue.getName(), bookings.get(0).getDate()),
                NotificationEntity.NotificationType.BOOKING
        );

        log.info("BookingGroup {} rejected by owner {}: {} bookings rejected", groupId, ownerId, results.size());
        return results;
    }

    // ─── checkInBooking ────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingDto checkInBooking(Long bookingId, Long ownerId) {
        log.info("BookingService.checkInBooking() called - bookingId={}, ownerId={}", bookingId, ownerId);
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getVenue().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        if (booking.getStatus() != BookingEntity.BookingStatus.CONFIRMED) {
            throw new ConflictException(
                    "Only CONFIRMED bookings can be checked in. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingEntity.BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getPlayer(),
                "Check-In Successful",
                String.format("You have been checked in at %s on %s (%s – %s). Enjoy your game!",
                        booking.getVenue().getName(), booking.getDate(),
                        booking.getStartTime(), booking.getEndTime()),
                NotificationEntity.NotificationType.BOOKING
        );

        log.info("Booking {} checked in by owner {}", bookingId, ownerId);
        return bookingMapper.toDto(booking);
    }

    // ─── checkInBookingGroup ───────────────────────────────────────────────

    @Override
    @Transactional
    public List<BookingDto> checkInBookingGroup(String groupId, Long ownerId) {
        log.info("BookingService.checkInBookingGroup() called - groupId={}, ownerId={}", groupId, ownerId);
        List<BookingEntity> bookings = bookingRepository.findByGroupIdOrderByStartTimeAsc(groupId);
        if (bookings.isEmpty()) {
            throw new ResourceNotFoundException("BookingGroup", "groupId", groupId);
        }

        VenueEntity venue = bookings.get(0).getVenue();
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        List<BookingDto> results = new ArrayList<>();
        for (BookingEntity booking : bookings) {
            if (booking.getStatus() != BookingEntity.BookingStatus.CONFIRMED) continue;
            booking.setStatus(BookingEntity.BookingStatus.COMPLETED);
            results.add(bookingMapper.toDto(bookingRepository.save(booking)));
        }

        if (results.isEmpty()) {
            throw new ConflictException("No CONFIRMED bookings found in group to check in");
        }

        UserEntity player = bookings.get(0).getPlayer();
        LocalDate date = bookings.get(0).getDate();
        String slotSummary = results.stream()
                .map(r -> r.getStartTime() + "–" + r.getEndTime())
                .reduce((a, b) -> a + ", " + b).orElse("");

        notificationService.createNotification(
                player,
                "Check-In Successful",
                String.format("You have been checked in for %d slot(s) at %s on %s (%s). Enjoy your game!",
                        results.size(), venue.getName(), date, slotSummary),
                NotificationEntity.NotificationType.BOOKING
        );

        log.info("BookingGroup {} checked in by owner {}: {} bookings updated", groupId, ownerId, results.size());
        return results;
    }

    // ─── adminListBookings ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BookingPage adminListBookings(int page, int size, String status) {
        log.info("BookingService.adminListBookings() called - status={}", status);
        Pageable pageable = PageRequest.of(page, size);
        BookingEntity.BookingStatus bookingStatus = parseStatus(status);
        Page<BookingEntity> entityPage = bookingRepository.findAllByStatus(bookingStatus, pageable);
        return toBookingPage(entityPage);
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private BookingEntity.BookingStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        try {
            return BookingEntity.BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid booking status: " + status);
        }
    }

    private BookingPage toBookingPage(Page<BookingEntity> entityPage) {
        BookingPage dto = new BookingPage();
        dto.setContent(entityPage.getContent().stream()
                .map(bookingMapper::toDto)
                .toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }
}
