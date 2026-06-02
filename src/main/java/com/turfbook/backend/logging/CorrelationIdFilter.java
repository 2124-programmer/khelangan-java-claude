package com.turfbook.backend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Runs first in the filter chain. Reads X-Correlation-Id from the incoming request
 * (or generates a UUID if absent), stores it in MDC so every log line in this thread
 * automatically carries it, and echoes it back on the response header.
 *
 * After the JWT filter has run and populated SecurityContext, userId and role are
 * also added to MDC for all downstream service/repo log lines.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_USER_ID        = "userId";
    public static final String MDC_ROLE           = "role";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {
            chain.doFilter(request, response);

            // After the JWT filter has run, enrich MDC with authenticated user info.
            // Safe to call here because the security filter chain runs inside doFilter.
            enrichMdcWithUser();
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ROLE);
        }
    }

    private void enrichMdcWithUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails ud) {
                MDC.put(MDC_USER_ID, ud.getUsername()); // username = email in UserPrincipal
                String role = auth.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .orElse("UNKNOWN");
                MDC.put(MDC_ROLE, role);
            }
        } catch (Exception ignored) {
            // Never let MDC enrichment break request processing
        }
    }
}
