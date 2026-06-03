-- ─── V5: Venue + Court schema for hourly slot engine ─────────────────────────
--
-- Venues gain: openTime/closeTime (hour-only strings), pricePerHour (renamed),
--              contactPhone, contactEmail, state, pincode, isActive.
-- Courts gain: nullable openTime/closeTime (NULL = inherit venue),
--              nullable pricePerHour (NULL = inherit venue), slotDurationMins, isActive.
-- Unique constraints: (owner_id, name) on venues; (venue_id, name) on courts.

-- ─── Venues ───────────────────────────────────────────────────────────────────
ALTER TABLE venues
  RENAME COLUMN price_per_slot TO price_per_hour;

ALTER TABLE venues
  ADD COLUMN contact_phone   VARCHAR(15)  NULL          AFTER city,
  ADD COLUMN contact_email   VARCHAR(255) NULL          AFTER contact_phone,
  ADD COLUMN state           VARCHAR(100) NULL          AFTER city,
  ADD COLUMN pincode         VARCHAR(10)  NULL          AFTER state,
  ADD COLUMN open_time       VARCHAR(5)   NOT NULL DEFAULT '05:00',
  ADD COLUMN close_time      VARCHAR(5)   NOT NULL DEFAULT '23:00',
  ADD COLUMN is_active       TINYINT(1)   NOT NULL DEFAULT 1;

ALTER TABLE venues
  ADD UNIQUE KEY uq_venues_owner_name (owner_id, name);

-- ─── Courts ───────────────────────────────────────────────────────────────────
-- Rename price column and make nullable (NULL means inherit venue price)
ALTER TABLE courts
  CHANGE COLUMN price_per_slot price_per_hour INT NULL DEFAULT NULL;

-- Make type optional (courts may omit it)
ALTER TABLE courts
  MODIFY COLUMN type VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE courts
  ADD COLUMN open_time          VARCHAR(5)   NULL,
  ADD COLUMN close_time         VARCHAR(5)   NULL,
  ADD COLUMN slot_duration_mins INT          NOT NULL DEFAULT 60,
  ADD COLUMN is_active          TINYINT(1)   NOT NULL DEFAULT 1;

ALTER TABLE courts
  ADD UNIQUE KEY uq_courts_venue_name (venue_id, name);

-- Existing courts created with explicit price 0 that also have a venue price set
-- are converted to NULL so they inherit the venue price going forward.
UPDATE courts c
  JOIN venues v ON c.venue_id = v.id
   SET c.price_per_hour = NULL
 WHERE c.price_per_hour = 0
   AND v.price_per_hour > 0;
