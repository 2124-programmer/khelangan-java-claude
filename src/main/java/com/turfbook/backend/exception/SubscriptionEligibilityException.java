package com.turfbook.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A subscription purchase / trial action was rejected because the venue is not eligible.
 * Carries a stable machine {@code code} (NO_COURTS, TRIAL_ALREADY_USED, ACTIVE_SUBSCRIPTION_EXISTS,
 * TOO_MANY_COURTS, NO_COURTS_SELECTED, COURT_NOT_IN_VENUE, REQUEST_ALREADY_PENDING) so the client
 * can branch on the reason, plus a human-readable message for the toast. Rendered as HTTP 409.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SubscriptionEligibilityException extends RuntimeException {

    private final String code;

    public SubscriptionEligibilityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
