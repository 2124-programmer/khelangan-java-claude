package com.turfbook.backend.service;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.dto.BookingPage;
import com.turfbook.backend.dto.BulkCreateBookingRequest;
import com.turfbook.backend.dto.CreateBookingRequest;
import com.turfbook.backend.entity.UserEntity;
import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    /**
     * @param date     exact date filter (used by "Today" tab); null = no exact filter
     * @param dateFrom lower-bound date filter inclusive (used by "Upcoming" tab); null = no bound
     */
    BookingPage listBookings(UserEntity currentUser, String status, LocalDate date, LocalDate dateFrom, int page, int size);

    BookingDto createBooking(Long playerId, CreateBookingRequest request);

    List<BookingDto> bulkCreateBookings(Long playerId, BulkCreateBookingRequest request);

    BookingDto getBooking(Long id, UserEntity currentUser);

    BookingDto cancelBooking(Long id, Long playerId);

    List<BookingDto> cancelBookingGroup(String groupId, Long playerId);

    BookingPage adminListBookings(int page, int size, String status);

    BookingDto acceptBooking(Long bookingId, Long ownerId);

    BookingDto rejectBooking(Long bookingId, Long ownerId);

    List<BookingDto> acceptBookingGroup(String groupId, Long ownerId);

    List<BookingDto> rejectBookingGroup(String groupId, Long ownerId);
}
