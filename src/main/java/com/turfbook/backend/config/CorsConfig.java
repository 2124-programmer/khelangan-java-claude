package com.turfbook.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /**
     * Comma-separated allowed origins, e.g. "https://admin.score-adda.com,https://score-adda.com".
     * Defaults to "*" for local dev. In prod set CORS_ALLOWED_ORIGINS to the real web origin(s);
     * native mobile clients don't send an Origin header so they're unaffected by this restriction.
     */
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    // Exposed as CorsConfigurationSource (not CorsFilter) so Spring Security can consume it
    // inside its own filter chain — before authorization checks run. A raw CorsFilter bean
    // without explicit ordering runs after Spring Security (order ~10^9 vs -100), which causes
    // OPTIONS preflights to be blocked before CORS headers are ever set.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // Blank or "*" → permissive (covers dev + native mobile, which sends no Origin).
        // Any explicit list → lock CORS to exactly those origins.
        if (origins.isEmpty() || origins.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
            log.warn("CORS is permissive (allowed-origins='*'). Set CORS_ALLOWED_ORIGINS to known "
                    + "hosts in production for stricter browser-origin enforcement.");
        } else {
            // allowedOriginPatterns accepts exact origins and wildcard patterns alike.
            config.setAllowedOriginPatterns(origins);
            log.info("CORS locked to origins: {}", origins);
        }

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        // JWT is passed in header (not cookie), so allowCredentials=false is correct
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
