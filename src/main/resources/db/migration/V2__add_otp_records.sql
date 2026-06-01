-- OTP record table for phone-based OTP login
CREATE TABLE IF NOT EXISTS otp_records (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    code_hash  VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hex of the 6-digit code',
    expires_at DATETIME(6)  NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    used       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_otp_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
