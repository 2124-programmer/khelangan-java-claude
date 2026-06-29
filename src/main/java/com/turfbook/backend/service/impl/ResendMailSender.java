package com.turfbook.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends email via Resend's HTTPS API (port 443) instead of SMTP. Cloud hosts like Railway block
 * outbound SMTP (25/465/587), so raw JavaMail to Gmail times out there; this transport works.
 * Enabled only when {@code RESEND_API_KEY} is set — otherwise MailServiceImpl falls back to SMTP
 * (fine for local dev). Uses the JDK HttpClient, so no extra dependency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResendMailSender {

    private static final String ENDPOINT = "https://api.resend.com/emails";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.mail.resend.api-key:}")
    private String apiKey;

    /**
     * From address for Resend — MUST be on a Resend-verified domain (e.g. "Score-Adda
     * &lt;noreply@score-adda.com&gt;"), or "onboarding@resend.dev" for testing (delivers only to the
     * Resend account owner). Distinct from the SMTP from, which can be the Gmail address.
     */
    @Value("${app.mail.from:}")
    private String from;

    /** True when a Resend API key is configured — MailServiceImpl uses this to pick the transport. */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Send one email. Throws on any non-2xx response so the caller logs + swallows like SMTP. */
    public void send(String to, String subject, String text, String html) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", List.of(to));
        body.put("subject", subject);
        if (html != null) body.put("html", html);
        if (text != null) body.put("text", text);

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            // Body carries Resend's reason (e.g. unverified domain, invalid key) — never the OTP.
            throw new IllegalStateException("Resend API returned " + response.statusCode() + ": " + response.body());
        }
    }
}
