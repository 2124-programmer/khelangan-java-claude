-- TurfBook Database Schema
-- Flyway migration V1

SET FOREIGN_KEY_CHECKS = 0;

-- ─── Users ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)   NOT NULL,
    email           VARCHAR(255)   NOT NULL,
    phone           VARCHAR(20)    NOT NULL,
    password_hash   VARCHAR(255)   NOT NULL,
    role            ENUM('PLAYER','OWNER','ADMIN') NOT NULL DEFAULT 'PLAYER',
    avatar_url      VARCHAR(500)   NULL,
    preferred_sports JSON          NULL,
    total_bookings  INT            NOT NULL DEFAULT 0,
    is_premium      TINYINT(1)     NOT NULL DEFAULT 0,
    is_blocked      TINYINT(1)     NOT NULL DEFAULT 0,
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_is_blocked (is_blocked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Sports ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sports (
    id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(100) NOT NULL,
    UNIQUE KEY uq_sports_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Venues ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS venues (
    id            BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id      BIGINT         NOT NULL,
    name          VARCHAR(200)   NOT NULL,
    address       VARCHAR(500)   NOT NULL,
    city          VARCHAR(100)   NOT NULL,
    description   TEXT           NULL,
    status        ENUM('PENDING','LIVE','REJECTED','SUSPENDED') NOT NULL DEFAULT 'PENDING',
    rating        DOUBLE         NOT NULL DEFAULT 0.0,
    review_count  INT            NOT NULL DEFAULT 0,
    price_per_slot INT           NOT NULL DEFAULT 0,
    cover_photo   VARCHAR(500)   NULL,
    photos        JSON           NULL,
    amenities     JSON           NULL,
    lat           DOUBLE         NOT NULL DEFAULT 0.0,
    lng           DOUBLE         NOT NULL DEFAULT 0.0,
    created_at    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_venues_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_venues_owner_id (owner_id),
    INDEX idx_venues_status (status),
    INDEX idx_venues_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Venue Sports (ManyToMany join) ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS venue_sports (
    venue_id BIGINT NOT NULL,
    sport_id BIGINT NOT NULL,
    PRIMARY KEY (venue_id, sport_id),
    CONSTRAINT fk_venue_sports_venue FOREIGN KEY (venue_id) REFERENCES venues (id) ON DELETE CASCADE,
    CONSTRAINT fk_venue_sports_sport FOREIGN KEY (sport_id) REFERENCES sports (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Courts ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS courts (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    venue_id       BIGINT       NOT NULL,
    name           VARCHAR(100) NOT NULL,
    sport_id       BIGINT       NOT NULL,
    type           VARCHAR(50)  NOT NULL,
    price_per_slot INT          NOT NULL DEFAULT 0,
    peak_price     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_courts_venue FOREIGN KEY (venue_id) REFERENCES venues (id) ON DELETE CASCADE,
    CONSTRAINT fk_courts_sport FOREIGN KEY (sport_id) REFERENCES sports (id) ON DELETE RESTRICT,
    INDEX idx_courts_venue_id (venue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Slots ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS slots (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    court_id   BIGINT      NOT NULL,
    date       DATE        NOT NULL,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    status     ENUM('AVAILABLE','BOOKED','BLOCKED') NOT NULL DEFAULT 'AVAILABLE',
    price      INT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_slots_court FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE,
    INDEX idx_slots_court_date (court_id, date),
    INDEX idx_slots_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Bookings ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookings (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id        BIGINT       NOT NULL,
    venue_id         BIGINT       NOT NULL,
    court_id         BIGINT       NOT NULL,
    slot_id          BIGINT       NOT NULL,
    sport            VARCHAR(100) NOT NULL,
    date             DATE         NOT NULL,
    start_time       TIME         NOT NULL,
    end_time         TIME         NOT NULL,
    amount           INT          NOT NULL DEFAULT 0,
    convenience_fee  INT          NOT NULL DEFAULT 20,
    discount         INT          NOT NULL DEFAULT 0,
    commission       INT          NOT NULL DEFAULT 0,
    status           ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    payment_status   ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    coupon_code      VARCHAR(50)  NULL,
    has_review       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_bookings_player  FOREIGN KEY (player_id)  REFERENCES users  (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_venue   FOREIGN KEY (venue_id)   REFERENCES venues (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_court   FOREIGN KEY (court_id)   REFERENCES courts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_slot    FOREIGN KEY (slot_id)    REFERENCES slots  (id) ON DELETE RESTRICT,
    INDEX idx_bookings_player_id (player_id),
    INDEX idx_bookings_venue_id (venue_id),
    INDEX idx_bookings_status (status),
    INDEX idx_bookings_date (date),
    INDEX idx_bookings_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Reviews ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT       NOT NULL,
    venue_id    BIGINT       NOT NULL,
    player_id   BIGINT       NOT NULL,
    player_name VARCHAR(100) NOT NULL,
    rating      INT          NOT NULL,
    comment     TEXT         NOT NULL,
    cleanliness INT          NOT NULL,
    ground      INT          NOT NULL,
    staff       INT          NOT NULL,
    owner_reply TEXT         NULL,
    created_at  DATE         NOT NULL DEFAULT (CURRENT_DATE),
    UNIQUE KEY uq_reviews_booking (booking_id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_venue   FOREIGN KEY (venue_id)   REFERENCES venues   (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_player  FOREIGN KEY (player_id)  REFERENCES users    (id) ON DELETE RESTRICT,
    INDEX idx_reviews_venue_id (venue_id),
    INDEX idx_reviews_player_id (player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Coupons ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupons (
    id             BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(50) NOT NULL,
    discount_type  ENUM('PERCENT','FLAT') NOT NULL,
    discount_value INT         NOT NULL,
    min_booking    INT         NOT NULL DEFAULT 0,
    max_discount   INT         NULL,
    valid_until    DATE        NOT NULL,
    used_count     INT         NOT NULL DEFAULT 0,
    max_uses       INT         NOT NULL DEFAULT 100,
    is_active      TINYINT(1)  NOT NULL DEFAULT 1,
    UNIQUE KEY uq_coupons_code (code),
    INDEX idx_coupons_is_active (is_active),
    INDEX idx_coupons_valid_until (valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Payouts ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payouts (
    id                   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id             BIGINT      NOT NULL,
    owner_name           VARCHAR(100) NOT NULL,
    amount               INT         NOT NULL,
    commission_deducted  INT         NOT NULL DEFAULT 0,
    net_amount           INT         NOT NULL,
    status               ENUM('PENDING','PROCESSING','SETTLED','FAILED') NOT NULL DEFAULT 'PENDING',
    date                 DATE        NOT NULL,
    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payouts_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_payouts_owner_id (owner_id),
    INDEX idx_payouts_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Disputes ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS disputes (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id     BIGINT       NOT NULL,
    player_id      BIGINT       NOT NULL,
    player_name    VARCHAR(100) NOT NULL,
    owner_id       BIGINT       NOT NULL,
    owner_name     VARCHAR(100) NOT NULL,
    venue_name     VARCHAR(200) NOT NULL,
    issue          TEXT         NOT NULL,
    status         ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
    resolved_note  TEXT         NULL,
    created_at     DATE         NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_disputes_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_player  FOREIGN KEY (player_id)  REFERENCES users    (id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_owner   FOREIGN KEY (owner_id)   REFERENCES users    (id) ON DELETE RESTRICT,
    INDEX idx_disputes_player_id (player_id),
    INDEX idx_disputes_owner_id (owner_id),
    INDEX idx_disputes_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Notifications ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT        NOT NULL,
    type       ENUM('BOOKING','PAYMENT','OFFER','SYSTEM','REVIEW') NOT NULL DEFAULT 'SYSTEM',
    is_read    TINYINT(1)  NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_is_read (is_read),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Platform Settings ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS platform_settings (
    id                   BIGINT     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    commission_percent   INT        NOT NULL DEFAULT 10,
    convenience_fee      INT        NOT NULL DEFAULT 20,
    maintenance_mode     TINYINT(1) NOT NULL DEFAULT 0,
    auto_approve_venues  TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default platform settings row (id=1, always singleton)
INSERT INTO platform_settings (id, commission_percent, convenience_fee, maintenance_mode, auto_approve_venues)
VALUES (1, 10, 20, 0, 0)
ON DUPLICATE KEY UPDATE id = id;

SET FOREIGN_KEY_CHECKS = 1;
