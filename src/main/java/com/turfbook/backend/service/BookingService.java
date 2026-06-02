package com.turfbook.backend.service;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.dto.BookingPage;
import com.turfbook.backend.dto.CreateBookingRequest;
import com.turfbook.backend.entity.UserEntity;

public interface BookingService {

    BookingPage listBookings(UserEntity currentUser, String status, int page, int size);

    BookingDto createBooking(Long playerId, CreateBookingRequest request);

    BookingDto getBooking(Long id, UserEntity currentUser);

    BookingDto cancelBooking(Long id, Long playerId);

    BookingPage adminListBookings(int page, int size, String status);

    BookingDto acceptBooking(Long bookingId, Long ownerId);

    BookingDto rejectBooking(Long bookingId, Long ownerId);
}
