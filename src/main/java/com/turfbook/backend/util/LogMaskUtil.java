package com.turfbook.backend.util;

public final class LogMaskUtil {

    private LogMaskUtil() {}

    /** s***@domain.com */
    public static String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** +91•••••3210 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "••••••••";
        int showSuffix = 4;
        int prefixLen = Math.min(3, phone.length() - showSuffix);
        String prefix = phone.substring(0, prefixLen);
        String suffix = phone.substring(phone.length() - showSuffix);
        String dots = "•".repeat(Math.max(0, phone.length() - prefixLen - showSuffix));
        return prefix + dots + suffix;
    }
}
