package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.dto.BookingPage;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    // ─── listBookings ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BookingPage listBookings(UserEntity currentUser, String status, int page, int size) {
        log.info("BookingService.listBookings() called - userId={}, role={}, status={}", currentUser.getId(), currentUser.getRole(), status);
        Pageable pageable = PageRequest.of(page, size);
        BookingEntity.BookingStatus bookingStatus = parseStatus(status);

        Page<BookingEntity> entityPage;

        switch (currentUser.getRole()) {
            case PLAYER -> {
                if (bookingStatus != null) {
                    entityPage = bookingRepository.findByPlayerAndStatusOrderByCreatedAtDesc(
                            currentUser, bookingStatus, pageable);
                } else {
                    entityPage = bookingRepository.findByPlayerOrderByCreatedAtDesc(currentUser, pageable);
                }
            }
            case OWNER -> entityPage = bookingRepository.findByVenueOwner(currentUser, bookingStatus, pageable);
            default -> entityPage = bookingRepository.findAllByStatus(bookingStatus, pageable);
        }

        return toBookingPage(entityPage);
    }

    // ─── createBooking ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingDto createBooking(Long playerId, CreateBookingRequest request) {
        log.info("BookingService.createBooking() called - playerId={}, slotId={}, venueId={}", playerId, request.getSlotId(), request.getVenueId());

        // 1. Load player
        UserEntity player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", playerId));

        // 2. Load and verify slot is AVAILABLE
        SlotEntity slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot", "id", request.getSlotId()));

        if (slot.getStatus() != SlotEntity.SlotStatus.AVAILABLE) {
            throw new ConflictException("Slot is not available for booking. Current status: " + slot.getStatus());
        }

        // 3. Load venue and court
        VenueEntity venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.getVenueId()));

        CourtEntity court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", request.getCourtId()));

        if (!court.getVenue().getId().equals(venue.getId())) {
            throw new IllegalArgumentException("Court does not belong to the specified venue");
        }

        if (!slot.getCourt().getId().equals(court.getId())) {
            throw new IllegalArgumentException("Slot does not belong to the specified court");
        }

        // 4. Platform settings
        PlatformSettingsEntity settings = settingsRepository.findById(1L)
                .orElseGet(() -> PlatformSettingsEntity.builder()
                        .id(1L)
                        .commissionPercent(10)
                        .convenienceFee(20)
                        .build());

        int slotPrice = slot.getPrice();
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

        // 7. Determine initial status based on owner's auto-accept setting
        UserEntity owner = venue.getOwner();
        boolean autoAccept = ownerSettingsService.isAutoAccept(owner.getId());

        if (autoAccept) {
            slot.setStatus(SlotEntity.SlotStatus.BOOKED);
        } else {
            slot.setStatus(SlotEntity.SlotStatus.HELD);
        }
        slotRepository.save(slot);

        // 8. Create booking
        BookingEntity booking = BookingEntity.builder()
                .player(player)
                .venue(venue)
                .court(court)
                .slot(slot)
                .sport(request.getSport())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
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
                    NotificationEntity.NotificationType.BOOKING
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
                    NotificationEntity.NotificationType.BOOKING
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

        SlotEntity slot = booking.getSlot();
        slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
        slotRepository.save(slot);

        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getPlayer(),
                "Booking Cancelled",
                String.format("Your booking at %s on %s has been cancelled.%s",
                        booking.getVenue().getName(),
                        booking.getDate(),
                        booking.getPaymentStatus() == BookingEntity.PaymentStatus.REFUNDED
                                ? " A refund has been initiated." : ""),
                NotificationEntity.NotificationType.BOOKING
        );

        return bookingMapper.toDto(booking);
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
