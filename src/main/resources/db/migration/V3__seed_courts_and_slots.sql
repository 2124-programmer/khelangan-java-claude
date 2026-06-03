-- Seed courts and slots for existing LIVE venues
-- Generates 1-hour slots 06:00–22:00 for 7 days starting 2026-06-02

-- ─── Jatra Turf (venue_id=4): Cricket + Football ─────────────────────────────

INSERT INTO courts (venue_id, name, sport_id, type, price_per_slot, peak_price)
VALUES
    (4, 'Cricket Court A', 1, 'OUTDOOR', 700, 900),
    (4, 'Cricket Court B', 1, 'OUTDOOR', 700, 900),
    (4, 'Football Ground A', 2, 'OUTDOOR', 800, 1000);

-- Slots for Cricket Court A
INSERT INTO slots (court_id, date, start_time, end_time, status, price)
WITH RECURSIVE days AS (
    SELECT DATE('2026-06-02') AS d
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE('2026-06-08')
),
hours AS (
    SELECT 6 AS h
    UNION ALL
    SELECT h + 1 FROM hours WHERE h < 21
)
SELECT c.id, d, MAKETIME(h, 0, 0), MAKETIME(h + 1, 0, 0), 'AVAILABLE', c.price_per_slot
FROM days
CROSS JOIN hours
CROSS JOIN (SELECT id, price_per_slot FROM courts WHERE venue_id = 4 AND name = 'Cricket Court A') c;

-- Slots for Cricket Court B
INSERT INTO slots (court_id, date, start_time, end_time, status, price)
WITH RECURSIVE days AS (
    SELECT DATE('2026-06-02') AS d
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE('2026-06-08')
),
hours AS (
    SELECT 6 AS h
    UNION ALL
    SELECT h + 1 FROM hours WHERE h < 21
)
SELECT c.id, d, MAKETIME(h, 0, 0), MAKETIME(h + 1, 0, 0), 'AVAILABLE', c.price_per_slot
FROM days
CROSS JOIN hours
CROSS JOIN (SELECT id, price_per_slot FROM courts WHERE venue_id = 4 AND name = 'Cricket Court B') c;

-- Slots for Football Ground A
INSERT INTO slots (court_id, date, start_time, end_time, status, price)
WITH RECURSIVE days AS (
    SELECT DATE('2026-06-02') AS d
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE('2026-06-08')
),
hours AS (
    SELECT 6 AS h
    UNION ALL
    SELECT h + 1 FROM hours WHERE h < 21
)
SELECT c.id, d, MAKETIME(h, 0, 0), MAKETIME(h + 1, 0, 0), 'AVAILABLE', c.price_per_slot
FROM days
CROSS JOIN hours
CROSS JOIN (SELECT id, price_per_slot FROM courts WHERE venue_id = 4 AND name = 'Football Ground A') c;

-- ─── Green turf (venue_id=2): Cricket ────────────────────────────────────────

INSERT INTO venue_sports (venue_id, sport_id) VALUES (2, 1);

INSERT INTO courts (venue_id, name, sport_id, type, price_per_slot, peak_price)
VALUES (2, 'Cricket Court A', 1, 'OUTDOOR', 500, 700);

INSERT INTO slots (court_id, date, start_time, end_time, status, price)
WITH RECURSIVE days AS (
    SELECT DATE('2026-06-02') AS d
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE('2026-06-08')
),
hours AS (
    SELECT 6 AS h
    UNION ALL
    SELECT h + 1 FROM hours WHERE h < 21
)
SELECT c.id, d, MAKETIME(h, 0, 0), MAKETIME(h + 1, 0, 0), 'AVAILABLE', c.price_per_slot
FROM days
CROSS JOIN hours
CROSS JOIN (SELECT id, price_per_slot FROM courts WHERE venue_id = 2 AND name = 'Cricket Court A') c;

-- ─── ower turf (venue_id=3): Cricket + Football ───────────────────────────────

INSERT INTO venue_sports (venue_id, sport_id) VALUES (3, 1), (3, 2);

INSERT INTO courts (venue_id, name, sport_id, type, price_per_slot, peak_price)
VALUES
    (3, 'Cricket Court A', 1, 'OUTDOOR', 500, 700),
    (3, 'Football Ground A', 2, 'OUTDOOR', 600, 800);

INSERT INTO slots (court_id, date, start_time, end_time, status, price)
WITH RECURSIVE days AS (
    SELECT DATE('2026-06-02') AS d
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE('2026-06-08')
),
hours AS (
    SELECT 6 AS h
    UNION ALL
    SELECT h + 1 FROM hours WHERE h < 21
)
SELECT c.id, d, MAKETIME(h, 0, 0), MAKETIME(h + 1, 0, 0), 'AVAILABLE', c.price_per_slot
FROM days
CROSS JOIN hours
CROSS JOIN (SELECT id, price_per_slot FROM courts WHERE venue_id = 3 AND name = 'Cricket Court A') c;

INSERT INTO slots (court_id, date, start_time, end_time, status, price)
WITH RECURSIVE days AS (
    SELECT DATE('2026-06-02') AS d
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE('2026-06-08')
),
hours AS (
    SELECT 6 AS h
    UNION ALL
    SELECT h + 1 FROM hours WHERE h < 21
)
SELECT c.id, d, MAKETIME(h, 0, 0), MAKETIME(h + 1, 0, 0), 'AVAILABLE', c.price_per_slot
FROM days
CROSS JOIN hours
CROSS JOIN (SELECT id, price_per_slot FROM courts WHERE venue_id = 3 AND name = 'Football Ground A') c;
