package com.turfbook.backend.exception;

/**
 * Raised (409) when a venue's active plan court limit blocks adding/keeping a court.
 * Carries the structured allowed/current counts so the client can render an upgrade prompt.
 */
public class CourtLimitExceededException extends ConflictException {

    private final int allowed;
    private final int current;
    private final String planName;

    public CourtLimitExceededException(String message, String planName, int allowed, int current) {
        super(message);
        this.planName = planName;
        this.allowed = allowed;
        this.current = current;
    }

    public int getAllowed() {
        return allowed;
    }

    public int getCurrent() {
        return current;
    }

    public String getPlanName() {
        return planName;
    }
}
