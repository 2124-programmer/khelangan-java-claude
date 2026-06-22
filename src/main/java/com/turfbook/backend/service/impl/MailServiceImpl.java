package com.turfbook.backend.service.impl;

import com.turfbook.backend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Override
    public void sendPasswordResetOtp(String toEmail, String otp, int expiresInMinutes) {
        String subject = "Score-Adda: Your password reset code";
        String body = "Your password reset code is: " + otp + "\n\n"
                + "This code expires in " + expiresInMinutes + " minutes. "
                + "Do not share it with anyone.\n\n"
                + "If you did not request a password reset, you can safely ignore this email.";
        send(toEmail, subject, body);
    }

    @Override
    public void sendEmailChangeVerificationOtp(String toEmail, String otp, int expiresInMinutes) {
        String subject = "Score-Adda: Verify your new email address";
        String body = "Your email verification code is: " + otp + "\n\n"
                + "This code expires in " + expiresInMinutes + " minutes. "
                + "Do not share it with anyone.\n\n"
                + "If you did not request an email change on Score-Adda, please ignore this email.";
        send(toEmail, subject, body);
    }

    @Override
    public void sendEmailChangeApproved(String toEmail, String newEmail) {
        String subject = "Score-Adda: Email address updated";
        String body = "Your account email has been successfully changed to: " + newEmail + "\n\n"
                + "If you did not make this request, please contact support immediately.";
        send(toEmail, subject, body);
    }

    @Override
    public void sendEmailChangeRejected(String toEmail, String newEmail, String reason) {
        String subject = "Score-Adda: Email change request declined";
        String body = "Your request to change your email to " + newEmail + " was not approved.\n\n"
                + "Reason: " + (reason != null ? reason : "No reason provided.") + "\n\n"
                + "If you have questions, please contact support.";
        send(toEmail, subject, body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Mail sent to={}", maskEmail(to));
        } catch (Exception ex) {
            log.error("Failed to send mail to={}: {}", maskEmail(to), ex.getMessage());
        }
    }

    private static String maskEmail(String email) {
        if (email == null) return "null";
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
