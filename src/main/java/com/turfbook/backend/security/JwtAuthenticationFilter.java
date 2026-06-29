package com.turfbook.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** The only endpoint a must-change-password user may call until they change it. */
    private static final String CHANGE_PASSWORD_PATH = "/api/v1/auth/change-password";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Refresh tokens are only valid at /auth/refresh — never for authenticating API calls.
                if (JwtTokenProvider.TYPE_REFRESH.equals(tokenProvider.getTokenType(jwt))) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Long userId = tokenProvider.getUserIdFromToken(jwt);
                int jwtVersion = tokenProvider.getTokenVersionFromToken(jwt);

                if (userId == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                Optional<UserEntity> userOpt = userRepository.findById(userId);
                // Invalidate tokens superseded by a password change or reset (tokenVersion bump).
                boolean versionValid = userOpt.map(u -> u.getTokenVersion() == jwtVersion).orElse(false);

                if (versionValid) {
                    UserEntity user = userOpt.get();

                    // Forced first-login password change: block EVERY authorized endpoint except the
                    // change-password call itself, server-side, so the change can't be skipped by
                    // calling the API directly. The flag is cleared on a successful change.
                    if (user.isMustChangePassword() && !isChangePasswordRequest(request)) {
                        writePasswordChangeRequired(request, response);
                        return; // stop the chain — do not authenticate or dispatch
                    }

                    UserDetails userDetails = userDetailsService.loadUserById(userId);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.debug("JWT rejected — tokenVersion mismatch for userId={}", userId);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }
        return null;
    }

    /** True only for POST /api/v1/auth/change-password — the one call allowed during a forced change. */
    private boolean isChangePasswordRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && CHANGE_PASSWORD_PATH.equals(request.getRequestURI());
    }

    /**
     * Short-circuit the request with a 403 carrying the stable code PASSWORD_CHANGE_REQUIRED so the
     * client can route to the change-password screen. Body mirrors the app's ErrorResponse shape.
     */
    private void writePasswordChangeRequired(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "PASSWORD_CHANGE_REQUIRED");
        body.put("message", "You must change your password before continuing.");
        body.put("path", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
