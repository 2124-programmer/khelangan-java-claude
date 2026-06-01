# TurfBook Backend — Complete API Documentation

> **Base URL:** `http://localhost:8080`  
> **API version:** 1.0.0  
> **Swagger UI:** `http://localhost:8080/swagger-ui.html`  
> **OpenAPI spec:** `http://localhost:8080/api-docs`

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Authentication](#authentication)
3. [Role System](#role-system)
4. [Error Response Format](#error-response-format)
5. [Pagination](#pagination)
6. [Auth APIs](#auth-apis)
7. [User APIs](#user-apis)
8. [Sports APIs](#sports-apis)
9. [Venue APIs](#venue-apis)
10. [Court APIs](#court-apis)
11. [Slot APIs](#slot-apis)
12. [Booking APIs](#booking-apis)
13. [Review APIs](#review-apis)
14. [Coupon APIs](#coupon-apis)
15. [Payout APIs](#payout-apis)
16. [Dispute APIs](#dispute-apis)
17. [Notification APIs](#notification-apis)
18. [Admin Statistics](#admin-statistics)
19. [Owner Statistics](#owner-statistics)
20. [Platform Settings APIs](#platform-settings-apis)
21. [Data Models Reference](#data-models-reference)
22. [Business Rules Summary](#business-rules-summary)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5.1 |
| Build | Maven |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Schema Migration | Flyway |
| API Contract | OpenAPI 3.0.3 (contract-first) |
| Code Generation | openapi-generator-maven-plugin 7.6.0 |
| Security | Spring Security 6, JWT (jjwt 0.12.6) |
| Mapping | MapStruct 1.6.3 + Lombok 1.18.36 |
| API Docs | springdoc-openapi 2.8.3 |

---

## Authentication

All endpoints except those marked **Public** require a Bearer JWT token.

**Header format:**
```
Authorization: Bearer <token>
```

**JWT payload claims:**
| Claim | Type | Description |
|---|---|---|
| `sub` | String | User ID (Long as string) |
| `role` | String | `PLAYER`, `OWNER`, or `ADMIN` |
| `iat` | Number | Issued-at timestamp |
| `exp` | Number | Expiry timestamp (issued + 24 hours) |

**Token lifetime:** 24 hours (configurable via `app.jwt.expiration-ms`).

**Refresh:** Use `POST /api/v1/auth/refresh` with the same token. In the current implementation the access and refresh tokens are identical; production should use a separate refresh-token store.

---

## Role System

| Role | Who | Access level |
|---|---|---|
| `PLAYER` | End users who book courts | Own profile, own bookings, reviews, disputes, coupons, notifications |
| `OWNER` | Venue owners | Own venues, courts, slots, stats, payouts, reviews of their venues |
| `ADMIN` | Platform administrators | All data, venue approvals, user management, settings, payouts, broadcast |

> **Note:** Self-registration as `ADMIN` is blocked. Admin accounts must be created directly in the database.

---

## Error Response Format

All errors return the following JSON body:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 409,
  "error": "CONFLICT",
  "message": "Slot is not available for booking. Current status: BOOKED",
  "path": "/api/v1/bookings",
  "fieldErrors": []
}
```

**Validation errors** populate `fieldErrors`:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "fieldErrors": [
    { "field": "email", "message": "must be a well-formed email address" },
    { "field": "password", "message": "size must be between 6 and 2147483647" }
  ]
}
```

**HTTP Status mapping:**

| Status | Scenario |
|---|---|
| `200` | Success (GET, PUT, PATCH) |
| `201` | Created (POST that creates a resource) |
| `204` | Deleted (no body) |
| `400` | Validation error / bad request |
| `401` | Missing or invalid JWT token |
| `403` | Authenticated but insufficient role |
| `404` | Resource not found |
| `409` | Conflict (duplicate email, slot not available, already reviewed, etc.) |
| `500` | Unexpected server error |

---

## Pagination

Endpoints returning lists use a page wrapper:

```json
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

**Default query parameters:**

| Param | Default | Description |
|---|---|---|
| `page` | `0` | Zero-based page index |
| `size` | `20` | Items per page |

---

## Auth APIs

### POST /api/v1/auth/register
**Access:** Public

Register a new user. ADMIN role cannot be self-registered (silently downgraded to PLAYER).

**Request body:**
```json
{
  "name": "Rahul Kumar",
  "email": "rahul@example.com",
  "phone": "9876543210",
  "password": "secret123",
  "role": "PLAYER"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | string | Yes | 2–100 characters |
| `email` | string | Yes | Valid email format |
| `phone` | string | Yes | 7–20 characters |
| `password` | string | Yes | Minimum 6 characters |
| `role` | string | Yes | `PLAYER` or `OWNER` |

**Business conditions:**
- Email must be unique across all users → `409 CONFLICT` if duplicate
- Email is stored lowercase and trimmed
- Password is BCrypt-hashed before storage
- `ADMIN` role in request is silently downgraded to `PLAYER`

**Response `201`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "name": "Rahul Kumar",
    "email": "rahul@example.com",
    "phone": "9876543210",
    "role": "PLAYER",
    "avatarUrl": null,
    "preferredSports": [],
    "totalBookings": 0,
    "isPremium": false,
    "isBlocked": false,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

---

### POST /api/v1/auth/login
**Access:** Public

Authenticate with email and password.

**Request body:**
```json
{
  "email": "rahul@example.com",
  "password": "secret123"
}
```

**Business conditions:**
- Email lookup is case-insensitive (lowercased before query)
- Password is verified against BCrypt hash
- If account `isBlocked = true` → `401` with message "Your account has been blocked. Please contact support."
- Spring Security `AuthenticationManager` handles the credential check → `401` on failure

**Response `200`:** Same as Register response.

---

### POST /api/v1/auth/otp/send
**Access:** Public

Request OTP to be sent to a phone number.

**Request body:**
```json
{ "phone": "9876543210" }
```

> **Note:** OTP delivery is a stub in the current implementation. Integrate with Twilio or AWS SNS in production.

**Response `200`:**
```json
{ "message": "OTP sent successfully to 9876543210" }
```

---

### POST /api/v1/auth/otp/verify
**Access:** Public

Verify OTP and receive a JWT token.

**Request body:**
```json
{
  "phone": "9876543210",
  "code": "123456"
}
```

**Business conditions:**
- Current implementation accepts any OTP code (demo mode)
- Looks up user by phone number → `401` if no user found
- Production: replace with real OTP verification

**Response `200`:** Same as Register response.

---

### POST /api/v1/auth/forgot-password
**Access:** Public

Request a password reset email.

**Request body:**
```json
{ "email": "rahul@example.com" }
```

> **Note:** Email sending is a stub. Response is always success to prevent email enumeration.

**Response `200`:**
```json
{ "message": "If an account with this email exists, a password reset link has been sent." }
```

---

### POST /api/v1/auth/refresh
**Access:** Public

Refresh a JWT token.

**Request body:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

**Business conditions:**
- Token must be valid and not expired → `401` if invalid
- User must still exist in database → `401` if deleted
- Returns a new 24-hour token

**Response `200`:** Same as Register response.

---

## User APIs

### GET /api/v1/users/me
**Access:** Any authenticated user

Get the currently authenticated user's profile.

**Response `200`:**
```json
{
  "id": 1,
  "name": "Rahul Kumar",
  "email": "rahul@example.com",
  "phone": "9876543210",
  "role": "PLAYER",
  "avatarUrl": "https://cdn.example.com/avatar.jpg",
  "preferredSports": ["Cricket", "Football"],
  "totalBookings": 5,
  "isPremium": false,
  "isBlocked": false,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

### PUT /api/v1/users/me
**Access:** Any authenticated user

Update the authenticated user's profile. All fields are optional (partial update).

**Request body:**
```json
{
  "name": "Rahul K.",
  "phone": "9999999999",
  "avatarUrl": "https://cdn.example.com/new-avatar.jpg",
  "preferredSports": ["Cricket", "Badminton"]
}
```

| Field | Type | Constraints |
|---|---|---|
| `name` | string | 2–100 characters |
| `phone` | string | Optional |
| `avatarUrl` | string | Optional URL |
| `preferredSports` | string[] | Optional list |

**Response `200`:** Updated `UserDto`.

---

### GET /api/v1/admin/users
**Access:** `ADMIN` only

List all users with optional filters.

**Query parameters:**

| Param | Type | Description |
|---|---|---|
| `page` | integer | Page index (default 0) |
| `size` | integer | Page size (default 20) |
| `role` | string | Filter by `PLAYER`, `OWNER`, `ADMIN` |
| `search` | string | Search by name or email |

**Response `200`:** `UserPage` — paginated list of `UserDto`.

---

### PATCH /api/v1/admin/users/{id}/block
**Access:** `ADMIN` only

Block a user. Blocked users cannot log in.

**Path parameter:** `id` — User ID (Long)

**Business conditions:**
- Sets `isBlocked = true` on the user entity
- Subsequent login attempts return `401`

**Response `200`:** Updated `UserDto` with `isBlocked: true`.

---

### PATCH /api/v1/admin/users/{id}/unblock
**Access:** `ADMIN` only

Unblock a previously blocked user.

**Path parameter:** `id` — User ID (Long)

**Response `200`:** Updated `UserDto` with `isBlocked: false`.

---

## Sports APIs

### GET /api/v1/sports
**Access:** Public

List all available sports.

**Response `200`:**
```json
[
  { "id": 1, "name": "Cricket", "icon": "🏏" },
  { "id": 2, "name": "Football", "icon": "⚽" },
  { "id": 3, "name": "Badminton", "icon": "🏸" }
]
```

---

### POST /api/v1/admin/sports
**Access:** `ADMIN` only

Create a new sport.

**Request body:**
```json
{ "name": "Tennis", "icon": "🎾" }
```

**Response `201`:** Created `SportDto`.

---

### PUT /api/v1/admin/sports/{id}
**Access:** `ADMIN` only

Update an existing sport.

**Path parameter:** `id` — Sport ID

**Request body:**
```json
{ "name": "Table Tennis", "icon": "🏓" }
```

**Response `200`:** Updated `SportDto`.

---

### DELETE /api/v1/admin/sports/{id}
**Access:** `ADMIN` only

Delete a sport.

**Path parameter:** `id` — Sport ID

**Response `204`:** No body.

---

## Venue APIs

### GET /api/v1/venues
**Access:** Public

List venues with status `LIVE` only. Supports filtering.

**Query parameters:**

| Param | Type | Description |
|---|---|---|
| `city` | string | Filter by city name |
| `sport` | string | Filter by sport name |
| `search` | string | Search by venue name |
| `page` | integer | Page index (default 0) |
| `size` | integer | Page size (default 20) |

**Business conditions:**
- Only `LIVE` venues are returned to public callers
- All three filters (`city`, `sport`, `search`) are applied with AND logic
- Omitted filters are ignored

**Response `200`:** `VenueSummaryPage`
```json
{
  "content": [
    {
      "id": 1,
      "name": "Champions Arena",
      "address": "123 Main St",
      "city": "Mumbai",
      "status": "LIVE",
      "rating": 4.5,
      "reviewCount": 12,
      "pricePerSlot": 800,
      "coverPhoto": "https://cdn.example.com/venue1.jpg",
      "lat": 19.0760,
      "lng": 72.8777,
      "ownerId": 5
    }
  ],
  "totalElements": 45,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

---

### POST /api/v1/venues
**Access:** `OWNER` only

Create a new venue. Venue starts in `PENDING` status pending Admin approval.

**Request body:**
```json
{
  "name": "Champions Arena",
  "address": "123 Main St, Andheri",
  "city": "Mumbai",
  "description": "Premium sports facility with 5 courts",
  "pricePerSlot": 800,
  "amenities": ["Parking", "Changing Rooms", "Cafeteria"],
  "lat": 19.0760,
  "lng": 72.8777,
  "sportIds": [1, 2],
  "coverPhoto": "https://cdn.example.com/cover.jpg",
  "photos": ["https://cdn.example.com/p1.jpg", "https://cdn.example.com/p2.jpg"],
  "courts": [
    {
      "name": "Court A",
      "sportId": 1,
      "type": "OUTDOOR",
      "pricePerSlot": 800,
      "peakPrice": 1200
    }
  ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | Yes | Venue name |
| `address` | string | Yes | Full address |
| `city` | string | Yes | City |
| `description` | string | No | Description |
| `pricePerSlot` | integer | Yes | Base price per slot (INR) |
| `amenities` | string[] | No | List of amenity strings |
| `lat` | double | Yes | Latitude |
| `lng` | double | Yes | Longitude |
| `sportIds` | Long[] | No | IDs of supported sports |
| `coverPhoto` | string | No | Cover photo URL |
| `photos` | string[] | No | Gallery photo URLs |
| `courts` | CreateCourtRequest[] | No | Courts to create inline |

**Business conditions:**
- Owner must exist in the system
- Each `sportId` must reference an existing sport → `404` if not found
- Venue is created with status `PENDING`
- Courts listed under `courts` are created as part of the same transaction

**Response `201`:** `VenueDetailDto`

---

### GET /api/v1/venues/{id}
**Access:** Public

Get full venue details including sports and courts.

**Path parameter:** `id` — Venue ID

**Response `200`:** `VenueDetailDto`
```json
{
  "id": 1,
  "name": "Champions Arena",
  "address": "123 Main St",
  "city": "Mumbai",
  "description": "Premium sports facility",
  "status": "LIVE",
  "rating": 4.5,
  "reviewCount": 12,
  "pricePerSlot": 800,
  "coverPhoto": "https://cdn.example.com/cover.jpg",
  "photos": ["https://cdn.example.com/p1.jpg"],
  "amenities": ["Parking", "Changing Rooms"],
  "lat": 19.0760,
  "lng": 72.8777,
  "ownerId": 5,
  "sports": [
    { "id": 1, "name": "Cricket", "icon": "🏏" }
  ],
  "courts": [
    { "id": 10, "venueId": 1, "name": "Court A", "sportId": 1, "type": "OUTDOOR", "pricePerSlot": 800, "peakPrice": 1200 }
  ],
  "createdAt": "2024-01-10T08:00:00Z"
}
```

**Error `404`:** Venue not found.

---

### PUT /api/v1/venues/{id}
**Access:** `OWNER` only (must own the venue)

Update venue details. All fields are optional (partial update).

**Path parameter:** `id` — Venue ID

**Request body:**
```json
{
  "name": "Champions Arena Pro",
  "description": "Updated description",
  "pricePerSlot": 900,
  "amenities": ["Parking", "Changing Rooms", "WiFi"],
  "coverPhoto": "https://cdn.example.com/new-cover.jpg",
  "photos": ["https://cdn.example.com/p3.jpg"]
}
```

**Business conditions:**
- Caller must be the venue owner → `403` if not
- Only non-null fields are updated (address and city cannot be changed via this endpoint)

**Response `200`:** Updated `VenueDetailDto`.

---

### PATCH /api/v1/venues/{id}/status
**Access:** `ADMIN` only

Change venue status.

**Path parameter:** `id` — Venue ID

**Request body:**
```json
{
  "status": "LIVE",
  "rejectionReason": ""
}
```

| Field | Type | Required | Values |
|---|---|---|---|
| `status` | string | Yes | `PENDING`, `LIVE`, `REJECTED`, `SUSPENDED` |
| `rejectionReason` | string | No | Reason text for rejection |

**Business conditions & side effects:**

| Transition | Side Effect |
|---|---|
| `PENDING → LIVE` | Owner receives notification: "Venue Approved!" |
| `ANY → REJECTED` | Owner receives notification: "Venue Rejected" with reason |
| `ANY → SUSPENDED` | No notification sent |

**Response `200`:** Updated `VenueDetailDto`.

---

### GET /api/v1/owner/venues
**Access:** `OWNER` only

List all venues owned by the authenticated owner (all statuses).

**Query params:** `page`, `size`

**Response `200`:** `VenueSummaryPage`

---

### GET /api/v1/admin/venues
**Access:** `ADMIN` only

List all venues across all owners with optional status filter.

**Query params:**

| Param | Type | Description |
|---|---|---|
| `page` | integer | Page index |
| `size` | integer | Page size |
| `status` | string | Filter by `PENDING`, `LIVE`, `REJECTED`, `SUSPENDED` |

**Response `200`:** `VenueSummaryPage`

---

## Court APIs

### GET /api/v1/venues/{venueId}/courts
**Access:** Public

List all courts for a given venue.

**Path parameter:** `venueId` — Venue ID

**Response `200`:**
```json
[
  {
    "id": 10,
    "venueId": 1,
    "name": "Court A",
    "sportId": 1,
    "type": "OUTDOOR",
    "pricePerSlot": 800,
    "peakPrice": 1200
  }
]
```

---

### POST /api/v1/venues/{venueId}/courts
**Access:** `OWNER` only (must own the venue)

Add a court to an existing venue.

**Path parameter:** `venueId` — Venue ID

**Request body:**
```json
{
  "name": "Court B",
  "sportId": 2,
  "type": "INDOOR",
  "pricePerSlot": 1000,
  "peakPrice": 1500
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | Yes | Court name |
| `sportId` | Long | Yes | ID of sport played on this court |
| `type` | string | Yes | e.g. `INDOOR`, `OUTDOOR` |
| `pricePerSlot` | integer | Yes | Price per slot (INR) |
| `peakPrice` | integer | No | Peak-hour price (default 0) |

**Business conditions:**
- Caller must own the venue → `403` if not
- `sportId` must exist → `404` if not found

**Response `201`:** `CourtDto`

---

### PUT /api/v1/venues/{venueId}/courts/{courtId}
**Access:** `OWNER` only (must own the venue)

Update court details. All fields are optional.

**Path parameters:** `venueId`, `courtId`

**Request body:**
```json
{
  "name": "Court B Premium",
  "pricePerSlot": 1100,
  "peakPrice": 1600,
  "type": "INDOOR",
  "sportId": 2
}
```

**Business conditions:**
- Caller must own the venue → `403` if not
- Court must belong to the specified venue → `404` if not found

**Response `200`:** Updated `CourtDto`.

---

### DELETE /api/v1/venues/{venueId}/courts/{courtId}
**Access:** `OWNER` only (must own the venue)

Delete a court.

**Business conditions:**
- Caller must own the venue → `403` if not
- Court must belong to the specified venue → `404` if not found

**Response `204`:** No body.

---

## Slot APIs

### GET /api/v1/courts/{courtId}/slots
**Access:** Public

List all slots for a court on a specific date, ordered by start time.

**Path parameter:** `courtId` — Court ID

**Query parameters:**

| Param | Type | Required | Description |
|---|---|---|---|
| `date` | string (date) | Yes | Date in `YYYY-MM-DD` format |

**Response `200`:**
```json
[
  {
    "id": 101,
    "courtId": 10,
    "date": "2024-01-20",
    "startTime": "09:00",
    "endTime": "10:00",
    "status": "AVAILABLE",
    "price": 800
  },
  {
    "id": 102,
    "courtId": 10,
    "date": "2024-01-20",
    "startTime": "10:00",
    "endTime": "11:00",
    "status": "BOOKED",
    "price": 800
  }
]
```

**Slot statuses:**

| Status | Meaning |
|---|---|
| `AVAILABLE` | Can be booked |
| `BOOKED` | Currently reserved by a booking |
| `BLOCKED` | Owner-blocked (not available for booking) |

---

### POST /api/v1/courts/{courtId}/slots/bulk-block
**Access:** `OWNER` only (must own the court's venue)

Block all AVAILABLE slots for a court on a given date.

**Path parameter:** `courtId` — Court ID

**Request body:**
```json
{ "date": "2024-01-25" }
```

**Business conditions:**
- Only slots currently in `AVAILABLE` status are blocked; `BOOKED` slots are untouched
- Caller must own the court's venue → `403` if not

**Response `200`:** List of all slots for that court+date after the operation.

---

### PATCH /api/v1/slots/{id}/block
**Access:** `OWNER` only (must own the slot's venue)

Block a single slot (sets status to `BLOCKED`).

**Path parameter:** `id` — Slot ID

**Business conditions:**
- Caller must own the slot's court's venue → `403` if not
- No restriction on current status (can block a previously unblocked slot)

**Response `200`:** Updated `SlotDto` with `status: "BLOCKED"`.

---

### PATCH /api/v1/slots/{id}/unblock
**Access:** `OWNER` only (must own the slot's venue)

Unblock a single slot (sets status back to `AVAILABLE`).

**Path parameter:** `id` — Slot ID

**Response `200`:** Updated `SlotDto` with `status: "AVAILABLE"`.

---

## Booking APIs

### GET /api/v1/bookings
**Access:** Any authenticated user

List bookings. Results are automatically scoped by the caller's role.

**Query parameters:**

| Param | Type | Description |
|---|---|---|
| `status` | string | Optional filter: `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED` |
| `page` | integer | Page index (default 0) |
| `size` | integer | Page size (default 20) |

**Role-based scoping:**

| Role | Returns |
|---|---|
| `PLAYER` | Only the player's own bookings, sorted newest first |
| `OWNER` | All bookings for all of the owner's venues |
| `ADMIN` | All bookings across the platform |

**Response `200`:** `BookingPage`

---

### POST /api/v1/bookings
**Access:** `PLAYER` only

Create a new booking.

**Request body:**
```json
{
  "venueId": 1,
  "courtId": 10,
  "slotId": 101,
  "sport": "Cricket",
  "couponCode": "SAVE20",
  "paymentMethod": "CARD"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `venueId` | Long | Yes | Venue ID |
| `courtId` | Long | Yes | Court ID (must belong to `venueId`) |
| `slotId` | Long | Yes | Slot ID (must belong to `courtId`) |
| `sport` | string | Yes | Sport name string |
| `couponCode` | string | No | Discount coupon code |
| `paymentMethod` | string | No | Default: `CARD` |

**Business logic — step by step:**

1. Load player entity (current user)
2. Load slot → if status ≠ `AVAILABLE` → `409 CONFLICT`
3. Verify `court.venue.id == venueId` → `400` if mismatch
4. Verify `slot.court.id == courtId` → `400` if mismatch
5. Load platform settings (fallback: commissionPercent=10, convenienceFee=20)
6. **Coupon evaluation** (if `couponCode` provided):
   - Look up coupon by code (uppercased, trimmed)
   - Coupon must be `active=true`
   - `validUntil` must not be in the past
   - `usedCount < maxUses`
   - `(slotPrice + convenienceFee) >= minBooking`
   - If `PERCENT` type: `discount = floor((amount × discountValue) / 100)`, capped at `maxDiscount`
   - If `FLAT` type: `discount = discountValue`, capped at total amount
   - Increment coupon `usedCount`
7. **Totals:**
   - `effectiveAmount = slotPrice + convenienceFee - discount`
   - `commission = floor((effectiveAmount × commissionPercent) / 100)`
8. Lock slot → status = `BOOKED`
9. Save booking with `status = CONFIRMED`, `paymentStatus = SUCCESS`
10. Increment `player.totalBookings`
11. Send notification to player: "Booking Confirmed"
12. Send notification to venue owner: "New Booking Received"

**Response `201`:** `BookingDto`
```json
{
  "id": 200,
  "playerId": 1,
  "playerName": "Rahul Kumar",
  "venueId": 1,
  "venueName": "Champions Arena",
  "courtId": 10,
  "courtName": "Court A",
  "slotId": 101,
  "sport": "Cricket",
  "date": "2024-01-20",
  "startTime": "09:00",
  "endTime": "10:00",
  "amount": 796,
  "convenienceFee": 20,
  "discount": 24,
  "commission": 79,
  "status": "CONFIRMED",
  "paymentStatus": "SUCCESS",
  "couponCode": "SAVE20",
  "hasReview": false,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Error `409`:** Slot already booked or blocked.

---

### GET /api/v1/bookings/{id}
**Access:** Any authenticated user

Get a single booking by ID.

**Path parameter:** `id` — Booking ID

**Access control:**

| Role | Can see |
|---|---|
| `PLAYER` | Only own bookings → `403` if booking belongs to another player |
| `OWNER` | Only bookings for own venues → `403` if venue belongs to another owner |
| `ADMIN` | Any booking |

**Response `200`:** `BookingDto`

---

### PATCH /api/v1/bookings/{id}/cancel
**Access:** `PLAYER` only (must own the booking)

Cancel a booking.

**Path parameter:** `id` — Booking ID

**Business logic:**

1. Booking must belong to the calling player → `403` if not
2. Booking `status` must be `CONFIRMED` → `409` if already cancelled or completed
3. Compute `hoursUntilSlot = hours between now and slot datetime`
4. **Refund policy:**
   - `hoursUntilSlot >= 24` → `paymentStatus = REFUNDED` (full refund)
   - `12 <= hoursUntilSlot < 24` → `paymentStatus = REFUNDED` (partial refund, handled at payout level)
   - `hoursUntilSlot < 12` → `paymentStatus` stays `SUCCESS` (no refund)
5. Set `booking.status = CANCELLED`
6. Free the slot → `status = AVAILABLE`
7. Send notification to player: "Booking Cancelled" (with refund note if applicable)

**Response `200`:** Updated `BookingDto` with `status: "CANCELLED"`.

---

### GET /api/v1/admin/bookings
**Access:** `ADMIN` only

List all bookings across the platform.

**Query params:** `page`, `size`, `status`

**Response `200`:** `BookingPage`

---

## Review APIs

### GET /api/v1/venues/{venueId}/reviews
**Access:** Public

List all reviews for a venue, sorted newest first.

**Path parameter:** `venueId` — Venue ID

**Query params:** `page` (default 0), `size` (default 20)

**Response `200`:** `ReviewPage`
```json
{
  "content": [
    {
      "id": 50,
      "bookingId": 200,
      "venueId": 1,
      "playerId": 1,
      "playerName": "Rahul Kumar",
      "rating": 5,
      "comment": "Excellent facility!",
      "cleanliness": 5,
      "ground": 4,
      "staff": 5,
      "ownerReply": null,
      "createdAt": "2024-01-21"
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

### POST /api/v1/reviews
**Access:** `PLAYER` only

Submit a review for a completed booking.

**Request body:**
```json
{
  "bookingId": 200,
  "rating": 5,
  "comment": "Excellent facility! Will come again.",
  "cleanliness": 5,
  "ground": 4,
  "staff": 5
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `bookingId` | Long | Yes | ID of the booking to review |
| `rating` | integer | Yes | 1–5 |
| `comment` | string | Yes | Review text |
| `cleanliness` | integer | Yes | 1–5 |
| `ground` | integer | Yes | 1–5 |
| `staff` | integer | Yes | 1–5 |

**Business conditions:**
- Booking must belong to the calling player → `403` if not
- Booking `status` must be `COMPLETED` → `409` if not completed
- `booking.hasReview` must be `false` → `409` if already reviewed

**Side effects:**
- Sets `booking.hasReview = true`
- Recalculates `venue.rating` (average of all ratings) and `venue.reviewCount`

**Response `201`:** `ReviewDto`

---

### GET /api/v1/owner/reviews
**Access:** `OWNER` only

List all reviews for all of the owner's venues.

**Query params:** `page`, `size`

**Response `200`:** `ReviewPage`

---

## Coupon APIs

### GET /api/v1/coupons
**Access:** Any authenticated user (typically `PLAYER`)

List currently active coupons (not expired, `isActive = true`).

**Response `200`:**
```json
[
  {
    "id": 1,
    "code": "SAVE20",
    "discountType": "PERCENT",
    "discountValue": 20,
    "minBooking": 500,
    "maxDiscount": 200,
    "validUntil": "2024-12-31",
    "usedCount": 5,
    "maxUses": 100,
    "isActive": true
  }
]
```

---

### POST /api/v1/coupons/validate
**Access:** Any authenticated user

Validate a coupon code against a booking amount and get the calculated discount.

**Request body:**
```json
{
  "code": "SAVE20",
  "bookingAmount": 820
}
```

**Validation logic:**
1. Coupon must exist → `valid: false` + "Invalid coupon code"
2. `isActive` must be `true` → `valid: false` + "Coupon is inactive"
3. `validUntil` must not be before today → `valid: false` + "Coupon has expired"
4. `usedCount < maxUses` → `valid: false` + "Coupon usage limit reached"
5. `bookingAmount >= minBooking` → `valid: false` + "Minimum booking amount of ₹X required"
6. Calculate discount (same formula as booking creation)

**Response `200`:**
```json
{
  "valid": true,
  "discount": 164,
  "message": "Coupon applied successfully! You save ₹164"
}
```

---

### GET /api/v1/admin/coupons
**Access:** `ADMIN` only

List all coupons (active and inactive).

**Query params:** `page`, `size`

**Response `200`:** `CouponPage`

---

### POST /api/v1/admin/coupons
**Access:** `ADMIN` only

Create a coupon.

**Request body:**
```json
{
  "code": "FLAT50",
  "discountType": "FLAT",
  "discountValue": 50,
  "minBooking": 400,
  "maxDiscount": null,
  "validUntil": "2024-12-31",
  "maxUses": 500
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `code` | string | Yes | Coupon code (stored uppercase) |
| `discountType` | string | Yes | `PERCENT` or `FLAT` |
| `discountValue` | integer | Yes | Percentage or flat amount |
| `minBooking` | integer | Yes | Minimum booking amount required |
| `maxDiscount` | integer | No | Cap on PERCENT discounts |
| `validUntil` | date | Yes | Expiry date `YYYY-MM-DD` |
| `maxUses` | integer | Yes | Maximum total uses |

**Business conditions:**
- Code is uppercased and trimmed before storage
- Code must be unique → `409` if duplicate
- `isActive` defaults to `true`

**Response `201`:** `CouponDto`

---

### PATCH /api/v1/admin/coupons/{id}
**Access:** `ADMIN` only

Toggle coupon active/inactive status.

**Path parameter:** `id` — Coupon ID

**Request body:**
```json
{ "isActive": false }
```

**Response `200`:** Updated `CouponDto`.

---

## Payout APIs

### GET /api/v1/owner/payouts
**Access:** `OWNER` only

List payouts for the authenticated owner.

**Query params:** `page`, `size`

**Response `200`:** `PayoutPage`
```json
{
  "content": [
    {
      "id": 1,
      "ownerId": 5,
      "ownerName": "Priya Sharma",
      "amount": 10000,
      "commissionDeducted": 1000,
      "netAmount": 9000,
      "status": "PENDING",
      "date": "2024-01-31",
      "createdAt": "2024-01-31T12:00:00Z"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**Payout statuses:**

| Status | Meaning |
|---|---|
| `PENDING` | Generated, waiting to be processed |
| `PROCESSED` | Payment transferred to owner |

---

### GET /api/v1/admin/payouts
**Access:** `ADMIN` only

List all payouts across all owners.

**Query params:** `page`, `size`, `status` (filter)

**Response `200`:** `PayoutPage`

---

### POST /api/v1/admin/payouts/{id}/process
**Access:** `ADMIN` only

Mark a payout as processed (transferred to owner).

**Path parameter:** `id` — Payout ID

**Response `200`:** Updated `PayoutDto` with `status: "PROCESSED"`.

---

## Dispute APIs

### GET /api/v1/disputes
**Access:** Any authenticated user

List disputes. Results are scoped by role.

**Query params:** `page`, `size`

**Role-based scoping:**

| Role | Returns |
|---|---|
| `PLAYER` | Disputes raised by the player |
| `OWNER` | Disputes against the owner's venues |
| `ADMIN` | All disputes |

**Response `200`:** `DisputePage`
```json
{
  "content": [
    {
      "id": 1,
      "bookingId": 200,
      "playerId": 1,
      "playerName": "Rahul Kumar",
      "ownerId": 5,
      "ownerName": "Priya Sharma",
      "venueName": "Champions Arena",
      "issue": "Court was not clean on arrival",
      "status": "OPEN",
      "resolvedNote": null,
      "createdAt": "2024-01-21"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**Dispute statuses:**

| Status | Meaning |
|---|---|
| `OPEN` | Under review |
| `RESOLVED` | Closed by Admin |

---

### POST /api/v1/disputes
**Access:** `PLAYER` only

Raise a dispute for a booking.

**Request body:**
```json
{
  "bookingId": 200,
  "issue": "Court was not clean on arrival and lights were not working"
}
```

**Business conditions:**
- Booking must belong to the calling player → `403` if not
- No restriction on booking status (can dispute CONFIRMED, COMPLETED, or CANCELLED)
- Dispute `status` starts as `OPEN`

**Side effects:**
- Venue owner receives notification: "New Dispute Raised"

**Response `201`:** `DisputeDto`

---

### PATCH /api/v1/disputes/{id}/resolve
**Access:** `ADMIN` only

Resolve an open dispute.

**Path parameter:** `id` — Dispute ID

**Request body:**
```json
{ "resolvedNote": "Refund issued and venue warned about cleanliness." }
```

**Business conditions:**
- Dispute `status` changes to `RESOLVED`
- `resolvedNote` is stored

**Side effects:**
- Player receives notification: "Dispute Resolved" with the note
- Venue owner receives notification: "Dispute Resolved" with the note

**Response `200`:** Updated `DisputeDto` with `status: "RESOLVED"`.

---

## Notification APIs

### GET /api/v1/notifications
**Access:** Any authenticated user

List notifications for the authenticated user, sorted newest first.

**Query params:** `page`, `size`

**Response `200`:** `NotificationPage`
```json
{
  "content": [
    {
      "id": 300,
      "userId": 1,
      "title": "Booking Confirmed",
      "body": "Your booking at Champions Arena on 2024-01-20 (09:00 – 10:00) is confirmed. Amount paid: ₹796",
      "type": "BOOKING",
      "isRead": false,
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**Notification types:**

| Type | Triggered by |
|---|---|
| `BOOKING` | Booking creation, cancellation |
| `SYSTEM` | Venue status changes, disputes, admin broadcast |

---

### PATCH /api/v1/notifications/{id}/read
**Access:** Any authenticated user (must own the notification)

Mark a single notification as read.

**Path parameter:** `id` — Notification ID

**Business conditions:**
- Notification must belong to the calling user → `403` if not

**Response `200`:** Updated `NotificationDto` with `isRead: true`.

---

### PATCH /api/v1/notifications/read-all
**Access:** Any authenticated user

Mark all notifications belonging to the authenticated user as read.

**Response `200`:**
```json
{ "message": "8 notifications marked as read" }
```

---

### POST /api/v1/admin/notifications/broadcast
**Access:** `ADMIN` only

Send a push notification to a targeted audience.

**Request body:**
```json
{
  "title": "Platform Maintenance",
  "body": "TurfBook will be under maintenance on Jan 20 from 2–4 AM.",
  "audience": "ALL"
}
```

| Field | Type | Required | Values |
|---|---|---|---|
| `title` | string | Yes | Notification title |
| `body` | string | Yes | Notification body text |
| `audience` | string | Yes | `ALL`, `PLAYERS`, `OWNERS` |

**Audience targeting:**

| Value | Targets |
|---|---|
| `ALL` | Every user in the system |
| `PLAYERS` | Users with `role = PLAYER` |
| `OWNERS` | Users with `role = OWNER` |

**Response `200`:**
```json
{ "message": "Broadcast sent to 245 users" }
```

---

## Admin Statistics

### GET /api/v1/admin/stats
**Access:** `ADMIN` only

Get platform-wide dashboard statistics for the current day.

**Response `200`:**
```json
{
  "bookingsToday": 24,
  "revenueToday": 19200,
  "newUsers": 7,
  "activeVenues": 38,
  "pendingApprovals": 3,
  "openDisputes": 2
}
```

| Field | Description |
|---|---|
| `bookingsToday` | Count of bookings created today |
| `revenueToday` | Sum of `amount` for bookings with `paymentStatus = SUCCESS` created today (INR) |
| `newUsers` | Count of users registered today |
| `activeVenues` | Count of venues with `status = LIVE` |
| `pendingApprovals` | Count of venues with `status = PENDING` |
| `openDisputes` | Count of disputes with `status = OPEN` |

---

## Owner Statistics

### GET /api/v1/owner/stats
**Access:** `OWNER` only

Get revenue statistics for the authenticated owner.

**Response `200`:**
```json
{
  "todayBookings": 3,
  "todayRevenue": 2400,
  "weekRevenue": 16800,
  "monthRevenue": 67200,
  "pendingPayout": 55000
}
```

| Field | Description |
|---|---|
| `todayBookings` | Count of confirmed bookings for owner's venues today |
| `todayRevenue` | Sum of booking amounts (SUCCESS) for owner's venues today (INR) |
| `weekRevenue` | Sum for last 7 days (INR) |
| `monthRevenue` | Sum from start of current month (INR) |
| `pendingPayout` | Total pending payout amount (INR) |

---

## Platform Settings APIs

### GET /api/v1/admin/settings
**Access:** `ADMIN` only

Get current platform configuration.

**Response `200`:**
```json
{
  "id": 1,
  "commissionPercent": 10,
  "convenienceFee": 20,
  "maintenanceMode": false,
  "autoApproveVenues": false
}
```

| Field | Description |
|---|---|
| `commissionPercent` | Platform commission on each booking (0–100) |
| `convenienceFee` | Fixed convenience fee added to every booking (INR) |
| `maintenanceMode` | Platform-wide maintenance flag |
| `autoApproveVenues` | Auto-approve new venue submissions without Admin review |

---

### PUT /api/v1/admin/settings
**Access:** `ADMIN` only

Update platform configuration. All fields are optional.

**Request body:**
```json
{
  "commissionPercent": 12,
  "convenienceFee": 25,
  "maintenanceMode": false,
  "autoApproveVenues": true
}
```

| Field | Type | Constraints |
|---|---|---|
| `commissionPercent` | integer | 0–100 |
| `convenienceFee` | integer | >= 0 |
| `maintenanceMode` | boolean | |
| `autoApproveVenues` | boolean | |

**Business conditions:**
- Settings record with `id = 1` is a singleton. If it doesn't exist, it is created with defaults (commissionPercent=10, convenienceFee=20, maintenanceMode=false, autoApproveVenues=false)
- Only non-null fields are updated

**Response `200`:** Updated `PlatformSettingsDto`.

---

## Data Models Reference

### UserDto
```json
{
  "id": "Long",
  "name": "string",
  "email": "string",
  "phone": "string",
  "role": "PLAYER | OWNER | ADMIN",
  "avatarUrl": "string | null",
  "preferredSports": ["string"],
  "totalBookings": "integer",
  "isPremium": "boolean",
  "isBlocked": "boolean",
  "createdAt": "ISO-8601 datetime"
}
```

### VenueSummaryDto
```json
{
  "id": "Long",
  "name": "string",
  "address": "string",
  "city": "string",
  "status": "PENDING | LIVE | REJECTED | SUSPENDED",
  "rating": "double",
  "reviewCount": "integer",
  "pricePerSlot": "integer",
  "coverPhoto": "string | null",
  "lat": "double",
  "lng": "double",
  "ownerId": "Long"
}
```

### VenueDetailDto
Extends `VenueSummaryDto` with:
```json
{
  "description": "string | null",
  "photos": ["string"],
  "amenities": ["string"],
  "sports": ["SportDto"],
  "courts": ["CourtDto"],
  "createdAt": "ISO-8601 datetime"
}
```

### BookingDto
```json
{
  "id": "Long",
  "playerId": "Long",
  "playerName": "string",
  "venueId": "Long",
  "venueName": "string",
  "courtId": "Long",
  "courtName": "string",
  "slotId": "Long",
  "sport": "string",
  "date": "YYYY-MM-DD",
  "startTime": "HH:mm",
  "endTime": "HH:mm",
  "amount": "integer (INR, final after discount)",
  "convenienceFee": "integer (INR)",
  "discount": "integer (INR)",
  "commission": "integer (INR, platform share)",
  "status": "PENDING | CONFIRMED | COMPLETED | CANCELLED",
  "paymentStatus": "PENDING | SUCCESS | FAILED | REFUNDED",
  "couponCode": "string | null",
  "hasReview": "boolean",
  "createdAt": "ISO-8601 datetime"
}
```

### SlotDto
```json
{
  "id": "Long",
  "courtId": "Long",
  "date": "YYYY-MM-DD",
  "startTime": "HH:mm",
  "endTime": "HH:mm",
  "status": "AVAILABLE | BOOKED | BLOCKED",
  "price": "integer (INR)"
}
```

### CouponDto
```json
{
  "id": "Long",
  "code": "string (uppercase)",
  "discountType": "PERCENT | FLAT",
  "discountValue": "integer",
  "minBooking": "integer (INR)",
  "maxDiscount": "integer | null (PERCENT cap)",
  "validUntil": "YYYY-MM-DD",
  "usedCount": "integer",
  "maxUses": "integer",
  "isActive": "boolean"
}
```

### NotificationDto
```json
{
  "id": "Long",
  "userId": "Long",
  "title": "string",
  "body": "string",
  "type": "BOOKING | SYSTEM",
  "isRead": "boolean",
  "createdAt": "ISO-8601 datetime"
}
```

### DisputeDto
```json
{
  "id": "Long",
  "bookingId": "Long",
  "playerId": "Long",
  "playerName": "string",
  "ownerId": "Long",
  "ownerName": "string",
  "venueName": "string",
  "issue": "string",
  "status": "OPEN | RESOLVED",
  "resolvedNote": "string | null",
  "createdAt": "YYYY-MM-DD"
}
```

### ReviewDto
```json
{
  "id": "Long",
  "bookingId": "Long",
  "venueId": "Long",
  "playerId": "Long",
  "playerName": "string",
  "rating": "integer (1–5)",
  "comment": "string",
  "cleanliness": "integer (1–5)",
  "ground": "integer (1–5)",
  "staff": "integer (1–5)",
  "ownerReply": "string | null",
  "createdAt": "YYYY-MM-DD"
}
```

### ErrorResponse
```json
{
  "timestamp": "ISO-8601 datetime",
  "status": "integer (HTTP status code)",
  "error": "string (error code)",
  "message": "string (human-readable description)",
  "path": "string (request URI)",
  "fieldErrors": [
    { "field": "string", "message": "string" }
  ]
}
```

---

## Business Rules Summary

### Booking Flow
```
Player selects Venue → Court → Slot (AVAILABLE)
  ↓
Coupon applied (optional)
  ↓
amount = slotPrice + convenienceFee - discount
commission = amount × commissionPercent / 100
  ↓
Slot locked (AVAILABLE → BOOKED)
Booking saved (status=CONFIRMED, paymentStatus=SUCCESS)
Player.totalBookings++
Notifications sent (player + owner)
```

### Cancellation Refund Policy
| Hours until slot start | Refund |
|---|---|
| >= 24 hours | Full refund (paymentStatus = REFUNDED) |
| 12–23 hours | Partial refund — 50% (paymentStatus = REFUNDED, handled at payout level) |
| < 12 hours | No refund (paymentStatus stays SUCCESS) |

In all cancellation cases:
- `booking.status → CANCELLED`
- `slot.status → AVAILABLE` (re-opens the slot for new bookings)

### Venue Lifecycle
```
Owner creates venue → status: PENDING
Admin reviews:
  PENDING → LIVE       (owner notified: approved)
  PENDING → REJECTED   (owner notified: rejected + reason)
  LIVE    → SUSPENDED  (no notification)
```

### Coupon Discount Calculation
```
PERCENT type:
  discount = floor(amount × discountValue / 100)
  if maxDiscount set: discount = min(discount, maxDiscount)

FLAT type:
  discount = discountValue
  discount = min(discount, amount)  ← never exceed the amount

A coupon is valid only when:
  isActive = true
  AND today <= validUntil
  AND usedCount < maxUses
  AND bookingAmount >= minBooking
```

### Review Eligibility
- Only the booking's player can submit a review
- Booking status must be `COMPLETED`
- Review can only be submitted once per booking (`hasReview` flag)
- Submitting a review updates the venue's `rating` (rolling average) and `reviewCount`

### Notification Triggers
| Event | Recipients |
|---|---|
| Booking created | Player + Venue Owner |
| Booking cancelled | Player |
| Venue approved | Venue Owner |
| Venue rejected | Venue Owner |
| Dispute created | Venue Owner |
| Dispute resolved | Player + Venue Owner |
| Admin broadcast | ALL / PLAYERS / OWNERS |

---

*Generated from source code — `turfbook-backend` v1.0.0*
