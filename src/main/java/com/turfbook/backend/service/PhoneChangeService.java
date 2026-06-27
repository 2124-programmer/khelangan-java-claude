package com.turfbook.backend.service;

import com.turfbook.backend.dto.*;

public interface PhoneChangeService {

    /** Step 1: user submits a new phone → sends a verification OTP to that number. */
    MessageResponse createRequest(Long userId, PhoneChangeCreateRequest request);

    /** Step 2: user verifies the OTP → the phone change is applied immediately (self-service). */
    PhoneChangeRequestDto verifyOtp(Long userId, PhoneChangeVerifyRequest request);

    /** Get the current/latest phone-change request for the user. */
    PhoneChangeRequestDto getStatus(Long userId);
}
