package com.turfbook.backend.service;

public interface MailService {

    void sendPasswordResetOtp(String toEmail, String otp, int expiresInMinutes);

    void sendEmailChangeVerificationOtp(String toEmail, String otp, int expiresInMinutes);

    void sendEmailChangeApproved(String toEmail, String newEmail);

    void sendEmailChangeRejected(String toEmail, String newEmail, String reason);
}
