package com.turfbook.backend.security;

import com.turfbook.backend.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coarse per-caller throttle on mutating requests (POST/PUT/PATCH/DELETE). Auth flows already have
 * their own fine-grained limits; this is a backstop so no single user/IP can hammer write endpoints
 * (booking, venue, court, etc.). Fixed 60s window, generous cap — only abusive bursts are blocked.
 *
 * NOTE: state is in-memory, so the limit is per-instance and resets on restart. For multi-instance
 * deployments move the counters to a shared store (Redis). See audit item B-12.
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final long WINDOW_MS = 60_000L;

    /** Max mutating requests per caller per 60s window. Overridable via config. */
    @Value("${app.ratelimit.writes-per-minute:120}")
    private int maxPerWindow;

    private static final class Window {
        volatile long startMs;
        final AtomicInteger count = new AtomicInteger(0);
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!MUTATING.contains(request.getMethod().toUpperCase())) {
            return true;
        }
        String key = callerKey(request);
        long now = System.currentTimeMillis();
        Window w = windows.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (now - w.startMs > WINDOW_MS) {
                w.startMs = now;
                w.count.set(0);
            }
        }
        int hits = w.count.incrementAndGet();
        if (hits > maxPerWindow) {
            log.warn("Rate limit exceeded for {} on {} {}", key, request.getMethod(), request.getRequestURI());
            throw new TooManyRequestsException("Too many requests. Please slow down and try again shortly.");
        }
        return true;
    }

    /** Throttle by authenticated user id when available, else by client IP. */
    private String callerKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal up) {
            return "u:" + up.getId();
        }
        String fwd = request.getHeader("X-Forwarded-For");
        String ip = (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : request.getRemoteAddr();
        return "ip:" + ip;
    }
}
