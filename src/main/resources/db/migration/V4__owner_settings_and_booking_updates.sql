-- ─── Extend slot status to include HELD (pending-booking hold) ────────────────
ALTER TABLE slots
    MODIFY COLUMN status ENUM('AVAILABLE','BOOKED','BLOCKED','HELD') NOT NULL DEFAULT 'AVAILABLE';

-- ─── Extend booking status to include REJECTED and EXPIRED ───────────────────
ALTER TABLE bookings
    MODIFY COLUMN status ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED','REJECTED','EXPIRED') NOT NULL DEFAULT 'PENDING';

-- ─── Owner-level booking/notification settings ────────────────────────────────
CREATE TABLE IF NOT EXISTS owner_settings (
    id                          BIGINT     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id                    BIGINT     NOT NULL,
    auto_accept_bookings        TINYINT(1) NOT NULL DEFAULT 0,
    push_notifications_enabled  TINYINT(1) NOT NULL DEFAULT 1,
    UNIQUE KEY uq_owner_settings_owner (owner_id),
    CONSTRAINT fk_owner_settings_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
