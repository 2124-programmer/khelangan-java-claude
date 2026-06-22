package com.turfbook.backend.service;

import com.turfbook.backend.dto.*;
import org.springframework.data.domain.Page;

public interface EmailChangeService {

    /** Step 1: owner submits new email → sends OTP to new email. */
    MessageResponse createRequest(Long userId, EmailChangeCreateRequest request);

    /** Step 2: owner verifies OTP → request moves to PENDING (admin queue). */
    EmailChangeRequestDto verifyOtp(Long userId, EmailChangeVerifyRequest request);

    /** Get the current/latest email-change request for the owner. */
    EmailChangeRequestDto getStatus(Long userId);

    /** Admin: list requests by status. */
    Page<EmailChangeRequestDto> adminList(String status, int page, int size);

    /** Admin: approve a request. */
    EmailChangeRequestDto adminApprove(Long requestId);

    /** Admin: reject a request. */
    EmailChangeRequestDto adminReject(Long requestId, EmailChangeRejectRequest request);
}
