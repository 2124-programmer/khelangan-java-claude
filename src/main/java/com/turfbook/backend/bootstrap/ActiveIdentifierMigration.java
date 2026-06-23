package com.turfbook.backend.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-time (idempotent) migration that moves user uniqueness from the raw {@code email}
 * column to the nullable-UNIQUE {@code active_email} / {@code active_phone} columns. This is
 * what lets an admin soft-delete an owner/player and free their email + phone for reuse: the
 * deleted row keeps {@code email}/{@code phone} for history while {@code active_*} is NULLed.
 *
 * <p>Runs as an {@link ApplicationRunner} (after Hibernate's {@code ddl-auto=update} has added
 * the new columns) so ordering is deterministic regardless of how Hibernate handles constraints:
 * <ol>
 *   <li>Backfill {@code active_email = LOWER(TRIM(email))} for every non-deleted account
 *       (existing emails are already unique, so this is collision-free).</li>
 *   <li>Backfill {@code active_phone = phone} only for the first account per phone number
 *       (phone was never unique historically, so duplicates are left NULL and logged — those
 *       users still log in by email; their phone is simply not reserved).</li>
 *   <li>Drop the legacy UNIQUE index on {@code users.email} so a deleted address can be reused.</li>
 *   <li>Create UNIQUE indexes on {@code active_email} / {@code active_phone} as the DB backstop
 *       (MySQL permits many NULLs, so deleted rows coexist with a fresh claim).</li>
 * </ol>
 * Every step is guarded so a benign re-run (or an already-applied state) never blocks startup.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class ActiveIdentifierMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        backfillActiveEmail();
        backfillActivePhone();
        dropLegacyEmailUniqueIndex();
        ensureUniqueIndex("uk_users_active_email", "active_email");
        ensureUniqueIndex("uk_users_active_phone", "active_phone");
    }

    private void backfillActiveEmail() {
        try {
            int n = jdbc.update(
                    "UPDATE users SET active_email = LOWER(TRIM(email)) " +
                    "WHERE active_email IS NULL AND status <> 'DELETED'");
            if (n > 0) log.info("ActiveIdentifierMigration: backfilled active_email for {} user(s).", n);
        } catch (Exception e) {
            log.warn("ActiveIdentifierMigration: active_email backfill skipped/failed: {}", e.getMessage());
        }
    }

    private void backfillActivePhone() {
        try {
            // Only the lowest-id account for each phone number claims active_phone; later
            // duplicates stay NULL (logged below) since phone was not unique historically.
            int n = jdbc.update(
                    "UPDATE users u " +
                    "JOIN (SELECT MIN(id) AS id FROM users " +
                    "      WHERE status <> 'DELETED' AND phone IS NOT NULL AND phone <> '' " +
                    "      GROUP BY phone) first ON u.id = first.id " +
                    "SET u.active_phone = u.phone " +
                    "WHERE u.active_phone IS NULL AND u.status <> 'DELETED'");
            if (n > 0) log.info("ActiveIdentifierMigration: backfilled active_phone for {} user(s).", n);

            Integer dupes = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE active_phone IS NULL AND active_email IS NOT NULL " +
                    "AND status <> 'DELETED' AND phone IS NOT NULL AND phone <> ''", Integer.class);
            if (dupes != null && dupes > 0) {
                log.warn("ActiveIdentifierMigration: {} account(s) have a duplicate phone and did NOT reserve " +
                        "active_phone (they still log in by email).", dupes);
            }
        } catch (Exception e) {
            log.warn("ActiveIdentifierMigration: active_phone backfill skipped/failed: {}", e.getMessage());
        }
    }

    private void dropLegacyEmailUniqueIndex() {
        try {
            List<String> indexes = jdbc.queryForList(
                    "SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' " +
                    "AND COLUMN_NAME = 'email' AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'",
                    String.class);
            for (String idx : indexes) {
                try {
                    jdbc.execute("ALTER TABLE users DROP INDEX `" + idx + "`");
                    log.info("ActiveIdentifierMigration: dropped legacy unique index '{}' on users.email.", idx);
                } catch (Exception e) {
                    log.warn("ActiveIdentifierMigration: could not drop index '{}': {}", idx, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("ActiveIdentifierMigration: legacy email-index lookup skipped/failed: {}", e.getMessage());
        }
    }

    private void ensureUniqueIndex(String indexName, String column) {
        try {
            jdbc.execute("CREATE UNIQUE INDEX " + indexName + " ON users (" + column + ")");
            log.info("ActiveIdentifierMigration: created unique index {} on users.{}.", indexName, column);
        } catch (Exception e) {
            // 1061 = duplicate key name → index already present; expected on re-runs.
            log.debug("ActiveIdentifierMigration: unique index {} already present or not created: {}",
                    indexName, e.getMessage());
        }
    }
}
