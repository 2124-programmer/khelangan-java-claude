package com.turfbook.backend.service;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.dto.CreateBookingRequest;
import com.turfbook.backend.entity.*;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.mapper.BookingMapper;
import com.turfbook.backend.repository.*;
import com.turfbook.backend.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private UserRepository userRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private PlatformSettingsRepository settingsRepository;
    @Mock private NotificationService notificationService;
    @Mock private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UserEntity player;
    private UserEntity owner;
    private VenueEntity venue;
    private CourtEntity court;
    private SlotEntity slot;
    private PlatformSettingsEntity settings;

    @BeforeEach
    void setUp() {
        owner = UserEntity.builder()
                .id(1L)
                .name("Venue Owner")
                .email("owner@test.com")
                .phone("1234567890")
                .passwordHash("hash")
                .role(UserEntity.Role.OWNER)
                .build();

        player = UserEntity.builder()
                .id(2L)
                .name("Test Player")
                .email("player@test.com")
                .phone("0987654321")
                .passwordHash("hash")
                .role(UserEntity.Role.PLAYER)
                .totalBookings(0)
                .build();

        venue = VenueEntity.builder()
                .id(10L)
                .owner(owner)
                .name("Test Turf")
                .address("123 Main St")
                .city("Mumbai")
                .pricePerSlot(500)
                .status(VenueEntity.VenueStatus.LIVE)
                .build();

        court = CourtEntity.builder()
                .id(20L)
                .venue(venue)
                .name("Court A")
                .type("OUTDOOR")
                .pricePerSlot(500)
                .peakPrice(700)
                .build();

        slot = SlotEntity.builder()
                .id(30L)
                .court(court)
                .date(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .status(SlotEntity.SlotStatus.AVAILABLE)
                .price(500)
                .build();

        settings = PlatformSettingsEntity.builder()
                .id(1L)
                .commissionPercent(10)
                .convenienceFee(20)
                .build();
    }

    @Test
    @DisplayName("createBooking: should create booking successfully when slot is AVAILABLE")
    void createBooking_Success() {
        // Given
        CreateBookingRequest request = new CreateBookingRequest();
        request.setVenueId(10L);
        request.setCourtId(20L);
        request.setSlotId(30L);
        request.setSport("Football");

        BookingEntity savedBooking = BookingEntity.builder()
                .id(100L)
                .player(player)
                .venue(venue)
                .court(court)
                .slot(slot)
                .sport("Football")
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .amount(520) // 500 + 20 - 0
                .convenienceFee(20)
                .discount(0)
                .commission(52) // 10% of 520
                .status(BookingEntity.BookingStatus.CONFIRMED)
                .paymentStatus(BookingEntity.PaymentStatus.SUCCESS)
                .build();

        BookingDto expectedDto = new BookingDto();
        expectedDto.setId(100L);
        expectedDto.setStatus("CONFIRMED");

        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(slotRepository.findById(30L)).thenReturn(Optional.of(slot));
        when(venueRepository.findById(10L)).thenReturn(Optional.of(venue));
        when(courtRepository.findById(20L)).thenReturn(Optional.of(court));
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        when(slotRepository.save(any(SlotEntity.class))).thenReturn(slot);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(savedBooking);
        when(userRepository.save(any(UserEntity.class))).thenReturn(player);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(expectedDto);
        doNothing().when(notificationService).createNotification(any(), any(), any(), any());

        // When
        BookingDto result = bookingService.createBooking(2L, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");

        verify(slotRepository).save(argThat(s -> s.getStatus() == SlotEntity.SlotStatus.BOOKED));
        verify(bookingRepository).save(any(BookingEntity.class));
        verify(notificationService, times(2)).createNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("createBooking: should throw ConflictException when slot is already BOOKED")
    void createBooking_SlotNotAvailable_ThrowsConflict() {
        // Given
        slot.setStatus(SlotEntity.SlotStatus.BOOKED);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setVenueId(10L);
        request.setCourtId(20L);
        request.setSlotId(30L);
        request.setSport("Football");

        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(slotRepository.findById(30L)).thenReturn(Optional.of(slot));

        // When / Then
        assertThatThrownBy(() -> bookingService.createBooking(2L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Slot is not available");

        verify(bookingRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("createBooking: should throw ConflictException when slot is BLOCKED")
    void createBooking_SlotBlocked_ThrowsConflict() {
        // Given
        slot.setStatus(SlotEntity.SlotStatus.BLOCKED);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setVenueId(10L);
        request.setCourtId(20L);
        request.setSlotId(30L);
        request.setSport("Tennis");

        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(slotRepository.findById(30L)).thenReturn(Optional.of(slot));

        // When / Then
        assertThatThrownBy(() -> bookingService.createBooking(2L, request))
                .isInstanceOf(ConflictException.class);

        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking: should set booking status to CANCELLED and free slot")
    void cancelBooking_Success_FullRefund() {
        // Given: slot is 2 days in future → full refund
        BookingEntity booking = BookingEntity.builder()
                .id(100L)
                .player(player)
                .venue(venue)
                .court(court)
                .slot(slot)
                .date(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .amount(520)
                .status(BookingEntity.BookingStatus.CONFIRMED)
                .paymentStatus(BookingEntity.PaymentStatus.SUCCESS)
                .build();

        BookingDto expectedDto = new BookingDto();
        expectedDto.setId(100L);
        expectedDto.setStatus("CANCELLED");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(slotRepository.save(any(SlotEntity.class))).thenReturn(slot);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(booking);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(expectedDto);
        doNothing().when(notificationService).createNotification(any(), any(), any(), any());

        // When
        BookingDto result = bookingService.cancelBooking(100L, 2L);

        // Then
        assertThat(result).isNotNull();
        verify(slotRepository).save(argThat(s -> s.getStatus() == SlotEntity.SlotStatus.AVAILABLE));
        verify(bookingRepository).save(argThat(b ->
                b.getStatus() == BookingEntity.BookingStatus.CANCELLED
                && b.getPaymentStatus() == BookingEntity.PaymentStatus.REFUNDED
        ));
    }

    @Test
    @DisplayName("cancelBooking: should throw ConflictException if booking is not CONFIRMED")
    void cancelBooking_NotConfirmed_ThrowsConflict() {
        // Given
        BookingEntity booking = BookingEntity.builder()
                .id(100L)
                .player(player)
                .venue(venue)
                .court(court)
                .slot(slot)
                .date(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .amount(520)
                .status(BookingEntity.BookingStatus.CANCELLED) // already cancelled
                .paymentStatus(BookingEntity.PaymentStatus.REFUNDED)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        // When / Then
        assertThatThrownBy(() -> bookingService.cancelBooking(100L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot cancel booking");

        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking: should throw UnauthorizedException if not booking owner")
    void cancelBooking_NotOwner_ThrowsUnauthorized() {
        // Given: booking belongs to player id=2, but trying to cancel as id=99
        BookingEntity booking = BookingEntity.builder()
                .id(100L)
                .player(player) // player.id = 2
                .venue(venue)
                .court(court)
                .slot(slot)
                .date(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .amount(520)
                .status(BookingEntity.BookingStatus.CONFIRMED)
                .paymentStatus(BookingEntity.PaymentStatus.SUCCESS)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        // When / Then
        assertThatThrownBy(() -> bookingService.cancelBooking(100L, 99L))
                .isInstanceOf(com.turfbook.backend.exception.UnauthorizedException.class);
    }

    @Test
    @DisplayName("createBooking: commission should be 10% of (slotPrice + convenienceFee - discount)")
    void createBooking_CommissionCalculation() {
        // Given: slot price=500, convenience=20, discount=0 → total=520, commission=52
        CreateBookingRequest request = new CreateBookingRequest();
        request.setVenueId(10L);
        request.setCourtId(20L);
        request.setSlotId(30L);
        request.setSport("Badminton");

        BookingEntity[] savedRef = new BookingEntity[1];
        BookingDto dto = new BookingDto();

        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(slotRepository.findById(30L)).thenReturn(Optional.of(slot));
        when(venueRepository.findById(10L)).thenReturn(Optional.of(venue));
        when(courtRepository.findById(20L)).thenReturn(Optional.of(court));
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        when(slotRepository.save(any())).thenReturn(slot);
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> {
            savedRef[0] = inv.getArgument(0);
            return savedRef[0];
        });
        when(userRepository.save(any())).thenReturn(player);
        when(bookingMapper.toDto(any())).thenReturn(dto);
        doNothing().when(notificationService).createNotification(any(), any(), any(), any());

        // When
        bookingService.createBooking(2L, request);

        // Then
        BookingEntity saved = savedRef[0];
        assertThat(saved).isNotNull();
        assertThat(saved.getAmount()).isEqualTo(520);      // 500 + 20
        assertThat(saved.getCommission()).isEqualTo(52);   // 10% of 520
        assertThat(saved.getConvenienceFee()).isEqualTo(20);
        assertThat(saved.getDiscount()).isEqualTo(0);
        assertThat(saved.getStatus()).isEqualTo(BookingEntity.BookingStatus.CONFIRMED);
        assertThat(saved.getPaymentStatus()).isEqualTo(BookingEntity.PaymentStatus.SUCCESS);
    }
}
