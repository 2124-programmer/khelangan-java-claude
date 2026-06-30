package com.turfbook.backend.service;

public interface MailService {

    void sendPasswordResetOtp(String toEmail, String otp, int expiresInMinutes);

    void sendEmailChangeVerificationOtp(String toEmail, String otp, int expiresInMinutes);

    /** OTP for a phone-number change, sent to the account's registered email. */
    void sendPhoneChangeVerificationOtp(String toEmail, String otp, int expiresInMinutes);

    void sendEmailChangeApproved(String toEmail, String newEmail);

    void sendEmailChangeRejected(String toEmail, String newEmail, String reason);

    /**
     * Warn an owner that a venue's trial/subscription ends in {@code daysRemaining} days, so they can
     * renew/activate before courts stop being bookable.
     *
     * @param trial true for a free-trial period, false for a paid subscription
     */
    void sendSubscriptionExpiryWarning(String toEmail, String venueName, int daysRemaining, boolean trial);

    /**
     * Remind an owner that a venue's subscription has ended (courts no longer visible to players) and
     * prompt them to purchase/renew to relist.
     */
    void sendSubscriptionExpiredReminder(String toEmail, String venueName);

    /**
     * Tell the player their booking was cancelled (their own action), noting whether a refund applies.
     * No-op when {@code toEmail} is blank.
     */
    void sendBookingCancelledToPlayer(String toEmail, String venueName, String date,
                                      String slotSummary, boolean refundInitiated);

    /**
     * Tell the venue owner that a player cancelled a booking and the slot(s) reopened.
     * No-op when {@code toEmail} is blank.
     */
    void sendBookingCancelledToOwner(String toEmail, String playerName, String venueName,
                                     String date, String slotSummary);
}
