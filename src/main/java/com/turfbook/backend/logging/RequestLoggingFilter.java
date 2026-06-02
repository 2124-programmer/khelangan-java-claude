package com.turfbook.backend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every HTTP request at INFO on entry and exit.
 * Runs after CorrelationIdFilter (order = HIGHEST_PRECEDENCE + 1) so correlationId
 * is already in MDC when we log.
 *
 * We intentionally do NOT log request/response bodies — they may contain PII.
 * We log: method, path, status, durationMs, correlationId (via MDC in the pattern).
 */
@Component
@Order(Integer.MIN_VALUE + 1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path   = request.getRequestURI();

        log.info("event=HTTP_REQUEST status=START method={} path={}", method, path);

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int  status     = response.getStatus();

            if (status >= 500) {
                log.error("event=HTTP_RESPONSE status=COMPLETE method={} path={} httpStatus={} durationMs={}",
                        method, path, status, durationMs);
            } else if (status >= 400) {
                log.warn("event=HTTP_RESPONSE status=COMPLETE method={} path={} httpStatus={} durationMs={}",
                        method, path, status, durationMs);
            } else {
                log.info("event=HTTP_RESPONSE status=COMPLETE method={} path={} httpStatus={} durationMs={}",
                        method, path, status, durationMs);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip logging for static assets / actuator health checks
        return path.startsWith("/actuator/health") || path.startsWith("/favicon.ico");
    }
}
