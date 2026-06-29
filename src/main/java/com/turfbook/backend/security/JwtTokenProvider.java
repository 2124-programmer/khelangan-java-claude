package com.turfbook.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /** Refresh tokens live much longer than access tokens. Default: 30 days. */
    @Value("${app.jwt.refresh-expiration-ms:2592000000}")
    private long jwtRefreshExpirationMs;

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Short-lived access token used to authenticate API requests. */
    public String generateToken(Long userId, String role, int tokenVersion) {
        return buildToken(userId, role, tokenVersion, TYPE_ACCESS, jwtExpirationMs);
    }

    /** Long-lived refresh token, exchanged at /auth/refresh for a fresh access+refresh pair. */
    public String generateRefreshToken(Long userId, String role, int tokenVersion) {
        return buildToken(userId, role, tokenVersion, TYPE_REFRESH, jwtRefreshExpirationMs);
    }

    private String buildToken(Long userId, String role, int tokenVersion, String type, long ttlMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("tv", tokenVersion)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /** Token type claim ("access"/"refresh"); null for legacy tokens issued before typing. */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    public int getTokenVersionFromToken(String token) {
        Claims claims = parseClaims(token);
        Integer tv = claims.get("tv", Integer.class);
        return tv != null ? tv : 0;
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("JWT signature invalid: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string empty: {}", ex.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
