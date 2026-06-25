package com.turfbook.backend.service;

import com.turfbook.backend.dto.*;

public interface EmailChangeService {

    /** Step 1: user submits new email → sends a verification OTP to that new email. */
    MessageResponse createRequest(Long userId, EmailChangeCreateRequest request);

    /** Step 2: user verifies the OTP → the email change is applied immediately (self-service). */
    EmailChangeRequestDto verifyOtp(Long userId, EmailChangeVerifyRequest request);

    /** Get the current/latest email-change request for the user. */
    EmailChangeRequestDto getStatus(Long userId);
}
