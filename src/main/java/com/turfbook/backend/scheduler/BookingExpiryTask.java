package com.turfbook.backend.scheduler;

import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Expires PENDING bookings that have not been actioned within 24 hours.
 * Expiry rule: if an owner does not accept or reject within 24h, the booking
 * transitions to EXPIRED and the held slot is released back to AVAILABLE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryTask {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;

    @Scheduled(fixedRate = 900_000) // runs every 15 minutes
    @Transactional
    public void cancelPastPendingBookings() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<BookingEntity> past = bookingRepository.findPastPendingBookings(today, now);
        if (past.isEmpty()) return;

        log.info("Auto-cancelling {} PENDING booking(s) whose slot has ended", past.size());
        for (BookingEntity booking : past) {
            try {
                booking.setStatus(BookingEntity.BookingStatus.CANCELLED);
                booking.setPaymentStatus(BookingEntity.PaymentStatus.PENDING);
                booking.setCancellationReason(BookingEntity.CancellationReason.TIME_OVER);
                SlotEntity slot = booking.getSlot();
                if (slot != null) {
                    slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
                    slotRepository.save(slot);
                }
                bookingRepository.save(booking);
                log.info("Booking {} auto-cancelled (time over) — slot ended {} {}", booking.getId(), booking.getDate(), booking.getEndTime());
            } catch (Exception e) {
                log.error("Failed to auto-cancel booking {} — skipping", booking.getId(), e);
            }
        }
    }

    @Scheduled(fixedRate = 3_600_000) // runs every hour
    @Transactional
    public void expirePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<BookingEntity> expired = bookingRepository.findExpiredPendingBookings(cutoff);
        if (expired.isEmpty()) return;

        log.info("Expiring {} PENDING booking(s) older than 24h", expired.size());
        for (BookingEntity booking : expired) {
            try {
                booking.setStatus(BookingEntity.BookingStatus.EXPIRED);
                SlotEntity slot = booking.getSlot();
                if (slot != null) {
                    slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
                    slotRepository.save(slot);
                    log.info("Booking {} expired — slot {} released", booking.getId(), slot.getId());
                } else {
                    log.warn("Booking {} expired — slot reference is missing (orphaned FK), booking expired without slot release", booking.getId());
                }
                bookingRepository.save(booking);
            } catch (Exception e) {
                log.error("Failed to expire booking {} — skipping this record", booking.getId(), e);
            }
        }
    }
}
