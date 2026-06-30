package com.turfbook.backend.booking;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.mapper.BookingMapper;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.CouponRepository;
import com.turfbook.backend.repository.PlatformSettingsRepository;
import com.turfbook.backend.repository.SlotRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.MailService;
import com.turfbook.backend.service.NotificationService;
import com.turfbook.backend.service.OwnerSettingsService;
import com.turfbook.backend.service.impl.BookingServiceImpl;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Player-cancellation emails: both the player and the venue owner must be notified by email when a
 * booking is cancelled. Pure-Mockito (no Spring context) so it verifies the wiring in isolation.
 */
@ExtendWith(MockitoExtension.class)
class BookingCancellationEmailTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private UserRepository userRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private PlatformSettingsRepository settingsRepository;
    @Mock private NotificationService notificationService;
    @Mock private BookingMapper bookingMapper;
    @Mock private OwnerSettingsService ownerSettingsService;
    @Mock private SubscriptionGate subscriptionGate;
    @Mock private MailService mailService;

    @InjectMocks private BookingServiceImpl bookingService;

    @Test
    void cancelBooking_emailsBothPlayerAndOwner() {
        UserEntity player = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        UserEntity owner = UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build();
        VenueEntity venue = VenueEntity.builder().id(10L).name("Turf X").owner(owner).build();
        SlotEntity slot = SlotEntity.builder()
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .status(SlotEntity.SlotStatus.BOOKED).price(500).build();
        BookingEntity booking = BookingEntity.builder()
                .id(100L).player(player).venue(venue).slot(slot)
                .date(LocalDate.now().plusDays(5)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .status(BookingEntity.BookingStatus.CONFIRMED)
                .paymentStatus(BookingEntity.PaymentStatus.SUCCESS)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(new BookingDto());

        bookingService.cancelBooking(100L, 1L);

        // Player learns the cancellation (refund applies — 5 days out, so > 12h → REFUNDED).
        verify(mailService).sendBookingCancelledToPlayer(
                eq("alice@example.com"), eq("Turf X"), anyString(), eq("09:00–10:00"), anyBoolean());
        // Owner learns a player cancelled and the slot reopened.
        verify(mailService).sendBookingCancelledToOwner(
                eq("bob@example.com"), eq("Alice"), eq("Turf X"), anyString(), eq("09:00–10:00"));
    }
}
