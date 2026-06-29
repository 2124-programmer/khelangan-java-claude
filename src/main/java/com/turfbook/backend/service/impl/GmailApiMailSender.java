package com.turfbook.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

/**
 * Sends email through the Gmail API over HTTPS (port 443) — so it works on hosts that block SMTP
 * (e.g. Railway) AND the message genuinely comes FROM the authorized Gmail account
 * (score.addda@gmail.com), with full Gmail deliverability. Enabled only when the three OAuth values
 * are configured; otherwise MailServiceImpl falls back to the next transport.
 *
 * <p>Auth: an offline OAuth2 refresh token (scope https://www.googleapis.com/auth/gmail.send) is
 * exchanged for short-lived access tokens, which are cached until expiry. Uses the JDK HttpClient
 * and the Jakarta Mail already on the classpath — no extra dependency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GmailApiMailSender {

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String SEND_ENDPOINT =
            "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.mail.gmail.client-id:}")
    private String clientId;

    @Value("${app.mail.gmail.client-secret:}")
    private String clientSecret;

    @Value("${app.mail.gmail.refresh-token:}")
    private String refreshToken;

    /** From header — the authorized Gmail (e.g. "Score-Adda <score.addda@gmail.com>"). */
    @Value("${app.mail.from:}")
    private String from;

    // Cached access token + its expiry epoch-ms (refreshed on demand; guarded by the lock below).
    private volatile String accessToken;
    private volatile long accessTokenExpiresAtMs;
    private final Object tokenLock = new Object();

    /** True when all OAuth values are present — MailServiceImpl uses this to pick the transport. */
    public boolean isEnabled() {
        return notBlank(clientId) && notBlank(clientSecret) && notBlank(refreshToken);
    }

    /** Send one email as the authorized Gmail account. Throws on any failure (caller logs + swallows). */
    public void send(String to, String subject, String text, String html) throws Exception {
        String raw = buildRawMessage(to, subject, text, html);
        String token = accessToken();

        String body = objectMapper.writeValueAsString(Map.of("raw", raw));
        HttpRequest request = HttpRequest.newBuilder(URI.create(SEND_ENDPOINT))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            // Body carries Gmail's reason (invalid grant, insufficient scope, etc.) — never the OTP.
            throw new IllegalStateException("Gmail API returned " + response.statusCode() + ": " + response.body());
        }
    }

    /** Build an RFC-2822 MIME message (text + optional HTML) and base64url-encode it for Gmail. */
    private String buildRawMessage(String to, String subject, String text, String html) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        MimeMessageHelper helper = new MimeMessageHelper(message, html != null, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        if (html != null) {
            helper.setText(text, html); // multipart/alternative: plain + HTML
        } else {
            helper.setText(text);
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        message.writeTo(buffer);
        return Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
    }

    /** Return a valid access token, refreshing via the refresh token when missing/expired. */
    private String accessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (accessToken != null && now < accessTokenExpiresAtMs) {
            return accessToken;
        }
        synchronized (tokenLock) {
            if (accessToken != null && System.currentTimeMillis() < accessTokenExpiresAtMs) {
                return accessToken; // another thread refreshed while we waited
            }
            String form = "client_id=" + enc(clientId)
                    + "&client_secret=" + enc(clientSecret)
                    + "&refresh_token=" + enc(refreshToken)
                    + "&grant_type=refresh_token";
            HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Gmail OAuth token refresh failed (" + response.statusCode() + "): " + response.body());
            }
            JsonNode json = objectMapper.readTree(response.body());
            accessToken = json.path("access_token").asText(null);
            long expiresInSec = json.path("expires_in").asLong(3600L);
            // Refresh 60s early to avoid using a token that expires mid-request.
            accessTokenExpiresAtMs = System.currentTimeMillis() + (expiresInSec - 60) * 1000L;
            if (accessToken == null) {
                throw new IllegalStateException("Gmail OAuth response had no access_token: " + response.body());
            }
            return accessToken;
        }
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
