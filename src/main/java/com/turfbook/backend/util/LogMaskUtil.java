package com.turfbook.backend.util;

/**
 * PII masking for log statements. Use before any identifier appears in a log line.
 * Rules: never log raw email, phone, password, OTP, or token.
 */
public final class LogMaskUtil {

    private LogMaskUtil() {}

    /**
     * s***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at); // includes the @
        if (local.length() <= 1) return "*" + domain;
        return local.charAt(0) + "***" + domain;
    }

    /**
     * +91•••••3210  (keeps first 3 chars + last 4)
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        int showSuffix = 4;
        int prefixLen = Math.min(3, phone.length() - showSuffix);
        String prefix = phone.substring(0, prefixLen);
        String suffix = phone.substring(phone.length() - showSuffix);
        String dots = "•".repeat(Math.max(0, phone.length() - prefixLen - showSuffix));
        return prefix + dots + suffix;
    }

    /**
     * Replaces token value with [REDACTED] — use for Authorization header values.
     */
    public static String maskToken(String token) {
        return token == null ? null : "[REDACTED]";
    }

    /**
     * Mask a generic secret (password, OTP, key).
     */
    public static String maskSecret(String secret) {
        return secret == null ? null : "[REDACTED]";
    }
}
