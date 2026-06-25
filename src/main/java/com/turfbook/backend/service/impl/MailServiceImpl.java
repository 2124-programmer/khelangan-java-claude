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
        log.info("Mail configured - host={}, port={}, from={}, passwordSet={}",
                mailHost, mailPort, maskEmail(fromAddress),
                mailPassword != null && !mailPassword.isBlank());
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

    /** Sends a text (+ optional HTML) message. Failures are swallowed after logging. */
    private void send(String to, String subject, String text, String html) {
        long startedAt = System.currentTimeMillis();
        log.info("Sending mail - to={}, subject='{}', html={}, host={}, port={}",
                maskEmail(to), subject, html != null, mailHost, mailPort);
        try {
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
            log.info("Mail sent - to={}, took={}ms", maskEmail(to), System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            // Full stack at error so SMTP/auth failures (e.g. 535 bad credentials, connection
            // timeouts, STARTTLS issues) are diagnosable. Subject/recipient only — never the body/code.
            log.error("Failed to send mail - to={}, subject='{}', error={}: {}",
                    maskEmail(to), subject, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    private static String maskEmail(String email) {
        if (email == null) return "null";
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
