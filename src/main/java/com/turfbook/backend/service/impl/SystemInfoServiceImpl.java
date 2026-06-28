package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.SystemInfoDto;
import com.turfbook.backend.service.AdminPermissionService;
import com.turfbook.backend.service.SystemInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the super-admin "App Configuration" snapshot. Reads values from the Spring {@link Environment}
 * and live JDBC metadata. Deliberately omits every secret (DB password, JWT secret, mail password).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemInfoServiceImpl implements SystemInfoService {

    private final Environment env;
    private final DataSource dataSource;
    private final AdminPermissionService adminPermissionService;

    private static final Pattern HOST_IN_JDBC = Pattern.compile("//([^/]+)/");

    @Override
    public SystemInfoDto getSystemInfo(Long actorId) {
        adminPermissionService.requireModerateHard(actorId); // SUPER_ADMIN only

        SystemInfoDto dto = new SystemInfoDto();

        String[] profiles = env.getActiveProfiles();
        dto.setEnvironment(profiles.length == 0 ? "default" : String.join(",", profiles));
        dto.setAppName(env.getProperty("spring.application.name", "Score-Adda Backend"));
        dto.setBaseUrl(env.getProperty("app.base-url"));
        dto.setServerPort(env.getProperty("server.port", "8080"));
        dto.setApiBasePath("/api/v1");

        dto.setDatabaseHost(parseHost(env.getProperty("spring.datasource.url")));
        dto.setMailHost(env.getProperty("spring.mail.host"));
        dto.setMailFrom(env.getProperty("spring.mail.username"));
        dto.setUploadDir(env.getProperty("app.upload.dir"));
        dto.setJwtExpiration(formatDuration(env.getProperty("app.jwt.expiration-ms")));

        dto.setJavaVersion(System.getProperty("java.version"));
        dto.setServerTimeZone(ZoneId.systemDefault().toString());
        dto.setServerTime(OffsetDateTime.now().toString());
        dto.setUptime(formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()));

        // Authoritative DB facts from the live connection (never includes credentials).
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            dto.setDatabaseName(c.getCatalog());
            dto.setDatabaseProduct(md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            if (dto.getDatabaseHost() == null) dto.setDatabaseHost(parseHost(md.getURL()));
        } catch (Exception e) {
            log.warn("SystemInfo: could not read DB metadata: {}", e.getMessage());
            dto.setDatabaseProduct("unavailable");
        }

        return dto;
    }

    /** Extract host:port from a JDBC URL, stripping any embedded credentials. */
    private String parseHost(String url) {
        if (url == null || url.isBlank()) return null;
        Matcher m = HOST_IN_JDBC.matcher(url);
        if (!m.find()) return null;
        String authority = m.group(1);
        int at = authority.lastIndexOf('@'); // drop user:pass@ if present
        return at >= 0 ? authority.substring(at + 1) : authority;
    }

    private String formatDuration(String millisStr) {
        if (millisStr == null) return null;
        try {
            long ms = Long.parseLong(millisStr.trim());
            long hours = ms / 3_600_000L;
            if (hours >= 24 && hours % 24 == 0) return (hours / 24) + " day(s)";
            return hours + " hour(s)";
        } catch (NumberFormatException e) {
            return millisStr;
        }
    }

    private String formatUptime(long ms) {
        long s = ms / 1000;
        long d = s / 86_400; s %= 86_400;
        long h = s / 3_600;  s %= 3_600;
        long m = s / 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0 || d > 0) sb.append(h).append("h ");
        sb.append(m).append("m");
        return sb.toString().trim();
    }
}
