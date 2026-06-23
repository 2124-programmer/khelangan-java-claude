package com.turfbook.backend.booking;

import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.entity.SportEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.NotificationRepository;
import com.turfbook.backend.repository.SlotRepository;
import com.turfbook.backend.repository.SportRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.BookingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that actioning a booking (from any surface) dismisses the owner's actionable
 * "New Booking Request" notification, so its Accept/Reject buttons no longer appear.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class BookingNotificationDismissTest {

    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private SportRepository sportRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CourtRepository courtRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private NotificationRepository notificationRepository;
    @PersistenceContext private EntityManager em;

    private UserEntity owner;
    private UserEntity player;
    private VenueEntity venue;
    private CourtEntity court;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(UserEntity.builder()
                .name("Owner").email("owner+" + System.nanoTime() + "@t.com").phone("9876543210")
                .passwordHash("h").role(UserEntity.Role.OWNER).build());
        player = userRepository.save(UserEntity.builder()
                .name("Player").email("player+" + System.nanoTime() + "@t.com").phone("9876500000")
                .passwordHash("h").role(UserEntity.Role.PLAYER).build());
        SportEntity sport = sportRepository.save(SportEntity.builder().name("Football").icon("⚽").build());
        venue = venueRepository.save(VenueEntity.builder()
                .owner(owner).name("Turf " + System.nanoTime()).address("1 St").city("Mumbai")
                .status(VenueEntity.VenueStatus.LIVE).pricePerHour(500).build());
        court = courtRepository.save(CourtEntity.builder()
                .venue(venue).sport(sport).name("C1").pricePerHour(500).slotDurationMins(60).build());
    }

    private BookingEntity pendingBooking() {
        SlotEntity slot = slotRepository.save(SlotEntity.builder()
                .court(court).date(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .status(SlotEntity.SlotStatus.HELD).price(500).build());
        return bookingRepository.save(BookingEntity.builder()
                .player(player).venue(venue).court(court).slot(slot).sport("Football")
                .date(slot.getDate()).startTime(slot.getStartTime()).endTime(slot.getEndTime())
                .amount(500).convenienceFee(0).discount(0).commission(0).hasReview(false)
                .status(BookingEntity.BookingStatus.PENDING)
                .paymentStatus(BookingEntity.PaymentStatus.PENDING).build());
    }

    private NotificationEntity requestNotif(BookingEntity booking) {
        return notificationRepository.save(NotificationEntity.builder()
                .user(owner).title("New Booking Request")
                .body("player requested a slot. Please accept or reject.")
                .type(NotificationEntity.NotificationType.BOOKING)
                .isRead(false)
                .referenceId(String.valueOf(booking.getId())).referenceType("BOOKING")
                .build());
    }

    @Test
    @DisplayName("acceptBooking dismisses the owner's request notification")
    void acceptDismissesRequestNotification() {
        BookingEntity booking = pendingBooking();
        NotificationEntity notif = requestNotif(booking);
        assertThat(notif.isRead()).isFalse();

        bookingService.acceptBooking(booking.getId(), owner.getId());

        // Bulk @Modifying update bypasses the persistence context — clear it to read fresh state.
        em.clear();
        assertThat(notificationRepository.findById(notif.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    @DisplayName("rejectBooking dismisses the owner's request notification")
    void rejectDismissesRequestNotification() {
        BookingEntity booking = pendingBooking();
        NotificationEntity notif = requestNotif(booking);

        bookingService.rejectBooking(booking.getId(), owner.getId());

        em.clear();
        assertThat(notificationRepository.findById(notif.getId()).orElseThrow().isRead()).isTrue();
    }
}
