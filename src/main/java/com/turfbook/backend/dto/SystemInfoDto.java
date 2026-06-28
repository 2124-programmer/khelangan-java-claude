package com.turfbook.backend.dto;

import lombok.Data;

/**
 * Non-sensitive runtime/configuration snapshot for the super-admin "App Configuration" screen.
 *
 * <p>SECURITY: this intentionally exposes ONLY diagnostic metadata. It must never carry secrets —
 * no DB password, JWT secret, or mail password. JDBC credentials are stripped from the host string.
 */
@Data
public class SystemInfoDto {
    private String appName;
    private String environment;      // active Spring profile(s), e.g. "default" / "prod"
    private String baseUrl;          // app.base-url
    private String serverPort;
    private String apiBasePath;      // "/api/v1"

    private String databaseName;
    private String databaseHost;     // host:port (credentials stripped)
    private String databaseProduct;  // e.g. "MySQL 8.0.45"

    private String mailHost;
    private String mailFrom;         // sender address (not a secret)
    private String uploadDir;
    private String jwtExpiration;    // human-readable, NOT the secret

    private String javaVersion;
    private String serverTimeZone;
    private String serverTime;       // ISO-8601 now
    private String uptime;           // human-readable process uptime
}
