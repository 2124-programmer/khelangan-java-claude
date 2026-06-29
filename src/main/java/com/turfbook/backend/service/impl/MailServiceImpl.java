package com.turfbook.backend.service.impl;

import com.turfbook.backend.service.MailService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Outbound mail. Every send is {@code @Async} (mailExecutor) so SMTP latency never blocks the
 * request thread, and best-effort: a delivery failure is logged (with the recipient masked and
 * never the code/contents) but does not propagate — callers still return their neutral response.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final GmailApiMailSender gmailApiMailSender;
    private final ResendMailSender resendMailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${spring.mail.host:?}")
    private String mailHost;

    @Value("${spring.mail.port:?}")
    private String mailPort;

    /** Injected only to report presence at startup — the value itself is never logged. */
    @Value("${spring.mail.password:}")
    private String mailPassword;

    /** One-line startup summary so you can confirm the env vars actually loaded (password never logged). */
    @PostConstruct
    void logMailConfig() {
        log.info("Mail configured - transport={}, host={}, port={}, from={}, passwordSet={}",
                activeTransport(), mailHost, mailPort, maskEmail(fromAddress),
                mailPassword != null && !mailPassword.isBlank());
    }

    /** Pick order: Gmail API → Resend → SMTP. Gmail/Resend use HTTPS (work where SMTP is blocked). */
    private String activeTransport() {
        if (gmailApiMailSender.isEnabled()) return "GmailAPI(HTTPS)";
        if (resendMailSender.isEnabled()) return "Resend(HTTPS)";
        return "SMTP";
    }

    @Override
    @Async("mailExecutor")
    public void sendPasswordResetOtp(String toEmail, String otp, int expiresInMinutes) {
        String subject = "Your Score-Adda password reset code";
        String text = "Your Score-Adda password reset code is: " + otp + "\n\n"
                + "This code expires in " + expiresInMinutes + " minutes. Do not share it with anyone.\n\n"
                + "If you didn't request this, you can safely ignore this email.";
        String html = otpHtml("Password reset code",
                "Use this code to reset your Score-Adda password:", otp, expiresInMinutes);
        send(toEmail, subject, text, html);
    }

    @Override
    @Async("mailExecutor")
    public void sendEmailChangeVerificationOtp(String toEmail, String otp, int expiresInMinutes) {
        String subject = "Verify your new Score-Adda email address";
        String text = "Your Score-Adda email verification code is: " + otp + "\n\n"
                + "This code expires in " + expiresInMinutes + " minutes. Do not share it with anyone.\n\n"
                + "If you didn't request an email change on Score-Adda, please ignore this email.";
        String html = otpHtml("Verify your email",
                "Use this code to confirm your new Score-Adda email address:", otp, expiresInMinutes);
        send(toEmail, subject, text, html);
    }

    @Override
    @Async("mailExecutor")
    public void sendPhoneChangeVerificationOtp(String toEmail, String otp, int expiresInMinutes) {
        String subject = "Verify your Score-Adda phone number change";
        String text = "Your Score-Adda phone change verification code is: " + otp + "\n\n"
                + "This code expires in " + expiresInMinutes + " minutes. Do not share it with anyone.\n\n"
                + "If you didn't request a phone number change on Score-Adda, please ignore this email.";
        String html = otpHtml("Verify your phone change",
                "Use this code to confirm the new phone number on your Score-Adda account:", otp, expiresInMinutes);
        send(toEmail, subject, text, html);
    }

    @Override
    @Async("mailExecutor")
    public void sendEmailChangeApproved(String toEmail, String newEmail) {
        String subject = "Your Score-Adda email address was updated";
        String text = "Your account email has been successfully changed to: " + newEmail + "\n\n"
                + "If you did not make this request, please contact support immediately.";
        send(toEmail, subject, text, null);
    }

    @Override
    @Async("mailExecutor")
    public void sendEmailChangeRejected(String toEmail, String newEmail, String reason) {
        String subject = "Your Score-Adda email change request was declined";
        String text = "Your request to change your email to " + newEmail + " was not approved.\n\n"
                + "Reason: " + (reason != null ? reason : "No reason provided.") + "\n\n"
                + "If you have questions, please contact support.";
        send(toEmail, subject, text, null);
    }

    /** Minimal branded HTML for an OTP email — kept simple/inline for deliverability. */
    private static String otpHtml(String heading, String intro, String otp, int expiresInMinutes) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;color:#0A1730\">"
                + "<h2 style=\"color:#0A1730;margin:0 0 12px\">" + heading + "</h2>"
                + "<p style=\"margin:0 0 16px;font-size:14px\">" + intro + "</p>"
                + "<div style=\"font-size:32px;font-weight:bold;letter-spacing:8px;"
                + "background:#F4F6FA;border-radius:8px;padding:16px;text-align:center;color:#0A1730\">"
                + otp + "</div>"
                + "<p style=\"margin:16px 0 0;font-size:13px;color:#445\">This code expires in "
                + expiresInMinutes + " minutes. Do not share it with anyone.</p>"
                + "<p style=\"margin:8px 0 0;font-size:12px;color:#889\">"
                + "If you didn't request this, you can safely ignore this email.</p>"
                + "</div>";
    }

    /**
     * Sends a text (+ optional HTML) message. Uses Resend's HTTPS API when configured (works on
     * Railway, which blocks SMTP), otherwise raw SMTP (fine for local dev). Failures are swallowed
     * after logging.
     */
    private void send(String to, String subject, String text, String html) {
        long startedAt = System.currentTimeMillis();
        String transport = activeTransport();
        log.info("Sending mail - to={}, subject='{}', html={}, transport={}",
                maskEmail(to), subject, html != null, transport);
        try {
            if (gmailApiMailSender.isEnabled()) {
                gmailApiMailSender.send(to, subject, text, html);
            } else if (resendMailSender.isEnabled()) {
                resendMailSender.send(to, subject, text, html);
            } else {
                sendViaSmtp(to, subject, text, html);
            }
            log.info("Mail sent - to={}, transport={}, took={}ms",
                    maskEmail(to), transport, System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            // Full stack at error so transport failures (Gmail invalid_grant/scope, Resend
            // unverified-domain/invalid-key, SMTP 535/timeouts) are diagnosable. Subject/recipient
            // only — never the body/code.
            log.error("Failed to send mail - to={}, subject='{}', transport={}, error={}: {}",
                    maskEmail(to), subject, transport, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    /** Raw SMTP transport (JavaMail) — the local-dev fallback when Resend isn't configured. */
    private void sendViaSmtp(String to, String subject, String text, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, html != null, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        if (html != null) {
            helper.setText(text, html); // multipart/alternative: plain + HTML
        } else {
            helper.setText(text);
        }
        mailSender.send(message);
    }

    private static String maskEmail(String email) {
        if (email == null) return "null";
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
