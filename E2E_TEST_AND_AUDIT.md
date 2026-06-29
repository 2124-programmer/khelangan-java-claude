# Score-Adda — End-to-End Test & Audit

Score-Adda is an Expo React Native + TypeScript app (`turfbook-claudeAI`) on a Spring Boot + MySQL backend (`turfbook-backend`), contract-first OpenAPI under `/api/v1`, JWT auth, roles Player / Owner / Admin / Super-admin, subscriptions-only revenue (players pay owners at the venue). This document tests every role's operations as runnable cases, then audits the app on Security, API-efficiency, and Cross-platform axes. **Every status is backed by a real file/endpoint** found in the code (verified against source on 2026-06-28). Legend: ✅ Done · 🟡 Improvement required · 🔴 Need to implement. Code is the source of truth; where the original brief disagreed with the code, the status follows the code and notes the brief.

---

## Section E — Status summary dashboard

| Area | ✅ Done | 🟡 Improvement | 🔴 To implement |
|---|---|---|---|
| A. E2E cases (by role) | 50 | 2 | 2 |
| B. Security | 12 | 4 | 0 |
| C. API efficiency | 6 | 0 | 0 |
| D. Cross-platform | 9 | 1 | 1 |
| **Total** | **77** | **7** | **3** |

> **Update (2026-06-28):** 10 audit items were implemented/resolved in this pass — see the
> "Resolved this pass" list below. Counts above reflect the new state.

### Resolved this pass ✅
| Was | Item | Resolution |
|---|---|---|
| 🟡 B-03 | Refresh-token rotation | Distinct typed access/refresh tokens; `/auth/refresh` now requires a refresh token, enforces `tokenVersion`, and rotates a new pair each call (`JwtTokenProvider`, `AuthServiceImpl.refreshToken`, `JwtAuthenticationFilter`). Server-side reuse-detection store still a future enhancement. |
| 🟡 B-16 | API rate-limiting on writes | `RateLimitInterceptor` throttles POST/PUT/PATCH/DELETE per user/IP (120/min) → 429; registered in `RateLimitConfig`. |
| 🟡 C-03 | Duplicate `/venues` + `/sports` | `useVenues` + `useInfiniteVenues` given `staleTime: 60_000` so remounts reuse cache (`useSports` already 10-min). |
| 🟡 C-04 | Slot cache staleness | `SlotSelectionScreen` re-fetches slots on screen focus (skips initial mount). |
| 🟡 C-06 | Notification polling | List poll → 60s + `refetchIntervalInBackground:false`; badge poll aligned to 60s. |
| 🟡 D-02 | Mixed SafeAreaView API | 28 player/owner/auth screens migrated to `react-native-safe-area-context` with `edges={['top']}` (codemod). |
| 🟡 D-03 | Keyboard avoidance | `KeyboardAvoidingView` added to Login/Register/OTP/ForgotPassword. |
| 🟡 D-08 | Android hardware-back | `useAndroidBack` hook + ForgotPassword steps back on hardware back. |
| 🟡 PL-16 | Change-phone flow | Already complete end-to-end (`PhoneChangeController` + `usePhoneChange`); earlier 🟡 was a false flag. |
| 🟡 OW-13 | Owner earnings/payouts | Endpoints exist & owner-scoped (`PayoutController.listOwnerPayouts`, `VenueController.getOwnerStats`); earlier 🟡 was a false flag. |
| 🟡 D-09 | Fonts + full dark mode | **Inter** bundled & applied app-wide (global Text/TextInput weight→family patch) for identical iOS/Android/web type; **full dark theme** via light/dark palettes resolved from the OS scheme at launch. Verified: tsc 0, Metro export OK. Follow-up: in-app Light/Dark/System toggle (reload-based) + web persistence fix (localStorage) + `userInterfaceStyle:"automatic"` so System detects device dark. |
| 🟡 B-14 | Debug API banner (Login) | Removed the `__DEV__` banner JSX, unused `BASE_URL` import, and `debugBanner` style. tsc clean. |
| 🟡 B-11 | CORS lockable for prod | `CorsConfig` now env-driven via `app.cors.allowed-origins` (`${CORS_ALLOWED_ORIGINS:*}`): explicit list locks origins, `*`/blank stays permissive for dev + native. `mvn compile` SUCCESS. |
| 🟡 B-13 | Admin-action audit logging | Extended the existing `admin_audit` trail to admin create/promote/role-change/remove (`UserServiceImpl`) and platform-settings updates (`AdminServiceImpl`, before→after in metadata). `mvn compile` SUCCESS. |
| 🟡→deferred D-05 | Android release signing | `build.gradle` release signing wired to Gradle-prop/env credentials (debug fallback); keystores gitignored; runbook `docs/RELEASE_SIGNING.md`. **Deferred ("coming soon")**: create keystore + register SHA-1 when release/Google Sign-In is scheduled. |

### Prioritized action list — remaining (High → Low)

**High**
| # | Item | Status | Where |
|---|---|---|---|
| 1 | Google Sign-In — listed as an auth method but unbuilt on both sides | 🔴 | PL-04 / D-04 |
| 2 | Owner-authored venue coupons shown on the player home venue card (today only global admin coupons exist) | 🔴 | OW-12 / PL-19 / AD-10 |
| 3 | CORS `allowedOrigins: "*"` → restrict to known hosts for prod | ✅ | B-11 (resolved — env-driven) |
| 4 | Remove `__DEV__` API-URL banner on Login (low effort) | ✅ | B-14 (resolved) |
| 5 | Android release signing / prod SHA-1 (blocks release + Google). **Deferred — "coming soon"** (scaffolding in place; keystore + SHA-1 to be done when release/Google Sign-In is scheduled) | 🟡 (deferred) | D-05 |

**Medium**
| # | Item | Status | Where |
|---|---|---|---|
| 6 | In-memory rate limiters → Redis/DB. **Accepted/deferred** — not needed at current scale (single instance, ~550 users); revisit only before scaling to 2+ instances | 🟡 (accepted) | B-12 |

**Low**
| # | Item | Status | Where |
|---|---|---|---|
| 7 | Admin-action audit logging | ✅ | B-13 (resolved — now covers settings + admin-role changes) |

---

## Section F — Pending items (detailed)

What exists today, why it matters, and what implementing it takes. Grouped 🔴 (build) then 🟡 (improve). **2 features + 5 improvements remain** (one of which, B-12, is an accepted/deferred decision); F7 below is now ✅ done. Effort key: S = hours, M = 1–3 days, L = ≥ a week.

### 🔴 To implement

#### F1 · Google Sign-In  — `PL-04`, `D-04` · Priority **High** · Effort **M**
- **Current state:** No social login anywhere. No `@react-native-google-signin/google-signin` / `expo-auth-session` dependency; no iOS URL scheme or Android SHA wiring; no backend OAuth / `id_token` verification endpoint (absent from `api.yaml`).
- **Why it matters:** Listed as a core auth method in the brief; reduces sign-in drop-off.
- **What it takes:**
  1. **FE** — add the Google Sign-In dependency; configure web/iOS/Android client IDs; add a "Continue with Google" button to `LoginScreen`/`RegisterScreen`.
  2. **Native** — iOS reversed-client-ID URL scheme; Android release SHA-1 (depends on F5).
  3. **BE** — `POST /api/v1/auth/google` that verifies the Google `id_token`, finds-or-creates the user (force non-ADMIN, set `active_email`), and returns the standard `AuthResponse`, reusing `AuthServiceImpl.issueTokens()`.
- **Blocked by:** F5 (Android needs a stable signing SHA-1).

#### F2 · Owner-authored venue coupons on the player home card  — `OW-12`, `PL-19`, `AD-10` · Priority **High** · Effort **M–L**
- **Current state:** A working but **global** coupon engine — admin CRUD (`/api/v1/admin/coupons`) + player list/validate (`/api/v1/coupons`, `/coupons/validate`) in `CouponServiceImpl`. `CouponEntity` has **no venue/owner FK**, there is **no owner endpoint**, and coupons are **not surfaced on venue cards**.
- **Why it matters:** The intended model is owners create offers for their own venue, shown on that venue's card on the player Home — a discovery/conversion driver. Today owners can't create offers at all.
- **What it takes:**
  1. **BE** — add `venue_id` (owner-derived) to `CouponEntity` + migration; owner endpoints `POST/PUT/GET /api/v1/owner/venues/{venueId}/coupons` with the same ownership guards as venues/courts; include the venue's active offer in the venue summary DTO.
  2. **FE owner** — an "Offers" screen under the venue to create/list/toggle coupons.
  3. **FE player** — render the active-offer badge on the venue card in `PlayerHomeScreen` + venue detail.
  4. **Policy** — decide whether admin keeps platform-wide promos (`AD-10`) or coupons become owner-only.
- **Note:** Supersedes the generic player Offers tab (`PL-19`) and the admin-only model (`AD-10`).

### 🟡 Improvements (open)

#### F3 · Lock CORS to known hosts  — `B-11` · Priority **High** · Effort **S** · ✅ **Resolved**
- **Was:** `CorsConfig` hardcoded `allowedOriginPatterns("*")`.
- **Done:** `CorsConfig` now reads `app.cors.allowed-origins` (`@Value`, default `*`), comma-split into a list:
  blank/`*` → permissive (covers dev + native mobile, which sends no Origin) and logs a warning; an
  explicit list → CORS locked to exactly those origins (logged at startup). Property added to
  `application.yml` + `application-prod.yml` as `${CORS_ALLOWED_ORIGINS:*}`. Methods/headers/`credentials:false`/`maxAge` unchanged. `mvn compile` SUCCESS.
- **To activate in prod:** set `CORS_ALLOWED_ORIGINS=https://admin.score-adda.com,...` in the prod env.

#### F4 · Remove the `__DEV__` API-URL banner  — `B-14` · Priority **High** · Effort **S (trivial)** · ✅ **Resolved**
- **Was:** `LoginScreen.tsx` rendered `API: {BASE_URL}` under `__DEV__` (already stripped from release builds).
- **Done:** Removed the banner JSX, the now-unused `BASE_URL` import, and the orphaned `debugBanner` style. `tsc` clean.

#### F5 · Android release signing / prod SHA-1  — `D-05` · Priority **High** · Effort **S–M** · 🟡 **Deferred — "coming soon"**
- **Status:** Scaffolding is in place; the keystore creation + SHA-1 registration are **intentionally deferred** until release / Google Sign-In is scheduled. Nothing blocks current dev/internal builds.
- **Done (code):** `android/app/build.gradle` `release` signingConfig reads `SCOREADDA_UPLOAD_STORE_FILE/_STORE_PASSWORD/_KEY_ALIAS/_KEY_PASSWORD` from Gradle props/env (falls back to debug when absent — no secrets committed). `.gitignore` excludes `*.keystore`/`*.jks`/`gradle.properties`. Full runbook in `turfbook-claudeAI/docs/RELEASE_SIGNING.md`.
- **When picked up (cannot be automated — secrets + external consoles):** create the upload keystore (`keytool` or `eas credentials`), provide the 4 credentials, register the **SHA-1** in Google Play Console + Google Cloud OAuth.
- **Note:** `android/` is gitignored (regenerated by prebuild/EAS) → **EAS-managed credentials is the authoritative path**; the Gradle block only helps one-off local builds. See the doc.

#### F6 · Admin-action audit logging  — `B-13` · Priority **Med** · Effort **M** · ✅ **Resolved**
- **Found:** The `admin_audit` table (`AdminAuditEntity`) + `AdminAuditRepository` + `audit()` helpers **already existed** and covered player/owner moderation (suspend/ban/unban/delete) in `AdminPlayerServiceImpl`/`AdminOwnerServiceImpl`.
- **Done (closed the gaps):** extended the trail to the two uncovered sensitive areas —
  - **Admin-account changes** (`UserServiceImpl`): `createAdmin` → `ADMIN_CREATE`, `promoteToAdmin` → `ADMIN_PROMOTE`, `setAdminRole` → `ADMIN_ROLE_CHANGE` (from→to role), `removeAdmin` → `ADMIN_DEACTIVATE`/`ADMIN_DEMOTE`; target = the affected admin.
  - **Platform settings** (`AdminServiceImpl.updateSettings`): `SETTINGS_UPDATE` with before→after of each changed field in `metadata`; self-referenced to the acting super-admin (settings have no user target, and the existing `target_user_id` column is NOT NULL — no schema change needed). `mvn compile` SUCCESS.

#### F7 · Custom fonts + full dark-mode theme  — `D-09` · ✅ **Done**
- **Fonts:** **Inter** bundled (`@expo-google-fonts/inter` + `expo-font`, SDK-51-pinned), loaded in `App.tsx` via `useFonts`, and applied app-wide by a global `Text`/`TextInput` render patch (`src/theme/applyFonts.ts`) that maps each element's `fontWeight` to the matching Inter family — so iOS, Android, and Chrome render the **same** font at correct weights with **zero per-screen edits**.
- **Dark mode:** light + dark palettes (same keys) in `src/theme/index.ts`; status bar theme-aware; non-adapting white backgrounds fixed.
- **In-app toggle:** `AppearanceSelector` (System / Light / Dark) in player Settings, owner Settings, and Admin Profile. The choice persists (`store/themePreference`, secure-store), is loaded by a startup gate (`RootGate` + custom `index.js` entry) **before** the theme resolves, and is re-applied instantly by reloading the JS app (`expo-updates` on native, `location.reload` on web).
- **Verified:** `tsc` 0 errors, `eslint` clean, **Metro `expo export` succeeds on Android + web** from the new entry (gate → lazy App), with all 4 Inter weights + `.ttf` assets bundled.
- **Note:** repaint happens via a ~1s app reload on toggle (static styles can't live-swap); switching the device theme while on "System" applies on next launch.

#### F8 · Rate-limiter durability (Redis)  — `B-12` · Priority **Low** · **Accepted / deferred**
- **Current:** In-memory `ConcurrentHashMap` limiters (auth flows + the new write limiter), per-instance, reset on restart.
- **Decision:** **Sufficient at current scale** — 2 admins / 50 owners / 500 players on a **single instance**. A restart just grants a fresh allowance (harmless).
- **Revisit when:** scaling to **2+ backend instances** (per-instance maps would let the cap leak). Then move counters to Redis/DB (or `bucket4j`). No code change needed before then.

---

## Section A — E2E test cases by role

### Player

#### [PL-01] Register — ✅
- **Precondition:** Guest; email + phone not already on an active account.
- **Steps:** 1) Open Register 2) enter name/email/phone/password, pick role 3) accept Terms 4) submit.
- **Expected result:** Account created (PLAYER), session + JWT issued, lands in Player tabs.
- **Negative cases:** Duplicate active email/phone → 409 routed inline to the field; password < 8 / no letter+digit → blocked; Terms unchecked → blocked; client sending `role=ADMIN` → forced to PLAYER server-side.
- **Code ref:** `turfbook-claudeAI/src/screens/auth/RegisterScreen.tsx` → `POST /api/v1/auth/register`; role forced in `AuthServiceImpl.register()` L106-114.
- **Notes:** Uniqueness checked on `active_email`/`active_phone`.

#### [PL-02] Login (email + password) — ✅
- **Precondition:** Active account.
- **Steps:** 1) Open Login 2) enter email + password 3) submit.
- **Expected result:** JWT + refresh stored, routed by role.
- **Negative cases:** Wrong password → 401; 5 failures / 15 min → account locked (retry-after); auth endpoints excluded from token auto-refresh.
- **Code ref:** `LoginScreen.tsx` → `POST /api/v1/auth/login`; `AuthServiceImpl.assertLoginNotLocked()`.
- **Notes:** —

#### [PL-03] Login via OTP — ✅
- **Precondition:** Account with reachable email/phone.
- **Steps:** 1) Login → request OTP 2) receive 6-digit code 3) enter on OTP screen → verify.
- **Expected result:** OTP verified, JWT issued.
- **Negative cases:** Wrong code → remaining-attempts shown, 5-attempt cap; expired (>10 min) → rejected; resend < 45 s → cooldown; > 5/hr → throttled.
- **Code ref:** `OTPVerificationScreen.tsx` → `POST /api/v1/auth/otp/send`, `/api/v1/auth/otp/verify`; OTP logic in `AuthServiceImpl`.
- **Notes:** Codes SHA-256-hashed at rest.

#### [PL-04] Login via Google — 🔴
- **Precondition:** —
- **Steps:** Tap "Continue with Google".
- **Expected result:** Google account → backend exchanges `id_token` → session.
- **Negative cases:** N/A (unbuilt).
- **Code ref:** No `@react-native-google-signin`/`expo-auth-session` dependency; no OAuth/`id_token` endpoint in backend or `api.yaml`.
- **Notes:** Brief lists Google Sign-In as an auth method — **not implemented on either side**. See D-04.

#### [PL-05] Forgot password (3-step email OTP) — ✅
- **Precondition:** Account exists (response is enumeration-safe regardless).
- **Steps:** 1) Enter email → request 2) enter 6-digit OTP → verify (returns single-use reset token) 3) set new password → confirm.
- **Expected result:** Password reset, all old sessions invalidated (`tokenVersion` bumped).
- **Negative cases:** Unknown email → generic success (no enumeration); wrong/expired OTP → rejected (10-min expiry, 5/hr); reset token single-use + 15-min expiry; new == old → blocked.
- **Code ref:** `ForgotPasswordScreen.tsx` → `POST /api/v1/auth/password-reset/{request,verify,confirm}`; `AuthSecurityController`.
- **Notes:** —

#### [PL-06] Search venues — ✅
- **Precondition:** Any (public).
- **Steps:** 1) Home 2) type query.
- **Expected result:** Debounced, paginated venue list.
- **Negative cases:** No matches → empty state; offline → query paused (NetInfo) and resumed.
- **Code ref:** `PlayerHomeScreen.tsx` (`useDebounce(query, 400)`, `useInfiniteVenues`) → `GET /api/v1/venues` (15/page).
- **Notes:** Input is debounced (see C-01).

#### [PL-07] Filter by sport — ✅
- **Precondition:** Sports loaded.
- **Steps:** 1) Open filter 2) pick sport 3) apply.
- **Expected result:** List filtered by sport param.
- **Negative cases:** Sport with no venues → empty state.
- **Code ref:** `PlayerHomeScreen.tsx` + `useSports` → `GET /api/v1/venues?...`, `GET /api/v1/sports`.
- **Notes:** —

#### [PL-08] Venue details — ✅
- **Precondition:** Venue is LIVE.
- **Steps:** Tap a venue card.
- **Expected result:** Detail with photos, sports, courts, reviews, contact.
- **Negative cases:** Non-LIVE venue not surfaced to players; deep-link to missing id → 404 state.
- **Code ref:** `VenueDetailScreen.tsx` (`useVenueDetail`, `useVenueReviews`) → `GET /api/v1/venues/{id}`, `GET /api/v1/venues/{id}/reviews`.
- **Notes:** —

#### [PL-09] Check slots — ✅
- **Precondition:** Venue with courts; date selected.
- **Steps:** 1) Venue → Book 2) pick court/date 3) view slots.
- **Expected result:** Slots grouped by court for the date (available/booked).
- **Negative cases:** Past date → no bookable slots; stale within 30 s cache (see C-04).
- **Code ref:** `SlotSelectionScreen.tsx` (`useVenueSlots`) → `GET /api/v1/venues/{venueId}/slots?date=&sport=`.
- **Notes:** —

#### [PL-10] Select adjacent-only slots — ✅
- **Precondition:** Multiple free slots on one court.
- **Steps:** Select two non-adjacent slots in one booking.
- **Expected result:** Adjacent multi-select allowed; non-adjacent blocked with guidance to book separately.
- **Negative cases:** Non-adjacent pick → "Adjacent slots only" alert; selection not added.
- **Code ref:** `SlotSelectionScreen.tsx:263-264`.
- **Notes:** Enforced client-side.

#### [PL-11] Book slot (auth-gate + returnTo) — ✅
- **Precondition:** Slot(s) selected; player may be guest.
- **Steps:** 1) Guest taps Book 2) redirected to Login 3) after login, resumes to confirm 4) confirm → booking request.
- **Expected result:** Booking(s) created PENDING (no in-app payment); returns to success.
- **Negative cases:** Unauth booking POST → blocked server-side (`hasRole('PLAYER')`); slot already taken → conflict.
- **Code ref:** `PaymentScreen.tsx` (`useBulkCreateBooking`) → `POST /api/v1/bookings/bulk`; auth-gate via `pendingNav.ts` returnTo.
- **Notes:** —

#### [PL-12] My bookings — ✅
- **Precondition:** Logged-in player.
- **Steps:** Bookings tab; switch status tabs.
- **Expected result:** Player's own bookings only, by status.
- **Negative cases:** Cannot read another user's bookings — list is server-scoped to the caller.
- **Code ref:** `MyBookingsScreen.tsx` (`useBookings`) → `GET /api/v1/bookings`; role-filtered in `BookingServiceImpl`.
- **Notes:** —

#### [PL-13] Cancel booking — ✅
- **Precondition:** Own upcoming booking (PENDING/CONFIRMED).
- **Steps:** Open booking → Cancel → confirm.
- **Expected result:** Booking cancelled (or group cancelled).
- **Negative cases:** Cancelling another player's booking → 403/ownership block; cancel non-cancellable status → rejected.
- **Code ref:** `MyBookingsScreen.tsx` → `PATCH /api/v1/bookings/{id}/cancel`, `/group/{groupId}/cancel`.
- **Notes:** —

#### [PL-14] Profile edit — ✅
- **Precondition:** Logged in.
- **Steps:** Profile → Edit → change name/avatar/preferred sports → save.
- **Expected result:** Profile updated; avatar uploaded.
- **Negative cases:** Body cannot set role/plan/status (not in `UpdateProfileRequest`); phone change ignored here (separate flow).
- **Code ref:** `MiscScreens.tsx` `EditProfileScreen` → `PUT /api/v1/users/me`, `POST /api/v1/users/avatar/upload`.
- **Notes:** —

#### [PL-15] Change email — ✅
- **Precondition:** Logged in.
- **Steps:** 1) Request email change 2) OTP to new email 3) verify → applied.
- **Expected result:** Email updated; profile cache refreshed.
- **Negative cases:** New email already active → conflict; wrong OTP → rejected.
- **Code ref:** `EmailChangeScreen.tsx` → `GET/POST /api/v1/users/email-change/{status,request,verify}`.
- **Notes:** —

#### [PL-16] Change phone — ✅
- **Precondition:** Logged in.
- **Steps:** Phone-change screen → request OTP (emailed) → verify.
- **Expected result:** Phone updated after OTP; `phone` + `active_phone` + `phoneVerified` set.
- **Negative cases:** Profile `PUT` intentionally ignores phone, forcing the OTP flow; new phone already on an active account → 409 (`existsByActivePhone`) at both request and verify; wrong/expired OTP → rejected.
- **Code ref:** `PhoneChangeScreen.tsx` + `usePhoneChange.ts` → `POST /api/v1/users/me/phone-change-requests`, `/verify`, `GET …/me`; `PhoneChangeController` + `PhoneChangeServiceImpl` (OTP SHA-256, expiry, uniqueness).
- **Notes:** Verified fully wired end-to-end (FE paths match BE). Earlier 🟡 was a false flag from the initial scan.

#### [PL-17] Delete account — ✅
- **Precondition:** Logged in; password re-auth.
- **Steps:** Settings → Delete account → enter password → confirm.
- **Expected result:** Soft-delete: status DELETED, `active_email`/`active_phone` nulled (freed for reuse), sessions force-logged-out, upcoming bookings cancelled.
- **Negative cases:** Wrong password → rejected.
- **Code ref:** `MiscScreens.tsx` `DeleteAccountScreen` → `POST /api/v1/users/delete`; `UserServiceImpl.deleteMe()`.
- **Notes:** See B-09.

#### [PL-18] Notifications — ✅
- **Precondition:** Logged in.
- **Steps:** Open Notifications; mark read / mark all read.
- **Expected result:** List with relative timestamps; unread badge updates.
- **Negative cases:** —
- **Code ref:** `NotificationsScreen.tsx` (`useNotifications`) → `GET /api/v1/notifications`, `PATCH /api/v1/notifications/{id}/read`.
- **Notes:** Polling cost — see C-06.

#### [PL-19] View offers / coupons — 🟡
- **Precondition:** Logged in.
- **Steps:** Offers tab → list active coupons; apply code at booking.
- **Expected result:** Active coupons listed; code validated (active/expiry/usage/min-booking/percent+flat).
- **Negative cases:** Invalid/expired/over-limit/below-min → rejected with reason.
- **Code ref:** `OffersScreen` + `BookingConfirmScreen.tsx` → `GET /api/v1/coupons`, `POST /api/v1/coupons/validate`; `CouponServiceImpl`.
- **Notes:** Engine works but coupons are **global**, not venue/owner-scoped, and aren't shown on the home venue card. Intended model is a 🔴 (PL/OW). *(brief said the coupon backend was not built; it partially is.)*

### Owner

#### [OW-01] Register / [OW-02] Login — ✅
- **Precondition:** As PL-01/PL-02 with role OWNER.
- **Steps:** Register choosing Owner → login.
- **Expected result:** Routed to Owner tabs.
- **Negative cases:** Same as PL-01/02.
- **Code ref:** `RegisterScreen.tsx`, `LoginScreen.tsx` → `/api/v1/auth/*`.
- **Notes:** —

#### [OW-03] Dashboard — ✅
- **Precondition:** Logged-in owner.
- **Steps:** Open Dashboard.
- **Expected result:** KPI summary + venues + today's bookings; quick check-in.
- **Negative cases:** —
- **Code ref:** `OwnerDashboardScreen.tsx` (`useOwnerDashboardSummary`) → `GET /api/v1/owner/dashboard/summary` (30 s staleTime).
- **Notes:** —

#### [OW-04] Create venue — ✅
- **Precondition:** Owner.
- **Steps:** Add Venue → details + sports + photos → save (DRAFT) → submit.
- **Expected result:** Venue created DRAFT, then PENDING on submit.
- **Negative cases:** Creating venue without OWNER role → 403.
- **Code ref:** `AddVenueScreen.tsx` → `POST /api/v1/venues` (`hasRole('OWNER')`), `POST /api/v1/venues/images/upload`.
- **Notes:** —

#### [OW-05] Edit / update venue — ✅
- **Precondition:** Owns the venue.
- **Steps:** Edit Venue → change fields → save.
- **Expected result:** Venue updated.
- **Negative cases:** **IDOR** — editing another owner's venue blocked server-side.
- **Code ref:** `EditVenueScreen` → `PUT /api/v1/venues/{id}`; ownership check in `VenueServiceImpl`.
- **Notes:** See B-05.

#### [OW-06] Submit / resubmit venue (CHANGES_REQUESTED) — ✅
- **Precondition:** Venue in DRAFT or CHANGES_REQUESTED.
- **Steps:** Address admin's reason → edit → submit.
- **Expected result:** Status → PENDING; re-enters approval queue.
- **Negative cases:** Submitting a venue you don't own → 403 (`VenueServiceImpl` submit ownership L508).
- **Code ref:** `EditVenueScreen` (`useSubmitVenue`) → `POST /api/v1/owner/venues/{venueId}/submit`.
- **Notes:** —

#### [OW-07] Court CRUD (plan-limit gate) — ✅
- **Precondition:** Owns venue; plan tier sets court cap (Trial→Pro Max 2/2/4/6/12).
- **Steps:** Court Management → add/edit/delete courts.
- **Expected result:** CRUD succeeds within plan cap.
- **Negative cases:** Exceeding cap → 409 `COURT_LIMIT_EXCEEDED` (allowed/current/plan) → upgrade prompt; editing another owner's court → 403.
- **Code ref:** `CourtManagementScreen.tsx` (`extractCourtLimit`) → `/api/v1/venues/{venueId}/courts` CRUD; ownership L851 (create) / L865 (update) in `VenueServiceImpl`.
- **Notes:** —

#### [OW-08] Booking requests + status tabs — ✅
- **Precondition:** Owns venues with bookings.
- **Steps:** Booking Mgmt → Requests/Today/Upcoming/Completed/Cancelled → accept/reject/check-in (single or group).
- **Expected result:** Status transitions applied; lists refresh.
- **Negative cases:** Acting on another owner's venue booking → 403; check-in before confirmed → blocked.
- **Code ref:** `OwnerScreens.tsx` `BookingManagementScreen` → `PATCH /api/v1/bookings/{id}/{accept,reject,check-in}`, `/group/{groupId}/*`.
- **Notes:** —

#### [OW-09] Subscription / plan view — ✅
- **Precondition:** Owner with a venue.
- **Steps:** Subscription screen.
- **Expected result:** Current plan, status, dates, available plans.
- **Negative cases:** Viewing another owner's subscription → 403 (service ownership check).
- **Code ref:** `OwnerScreens.tsx` `SubscriptionScreen` → `GET /api/v1/owner/subscriptions/{venueId}`, `/plans`.
- **Notes:** —

#### [OW-10] Upgrade prompt / request — ✅
- **Precondition:** On a tier below cap need.
- **Steps:** Hit cap or tap Upgrade → choose plan → request.
- **Expected result:** Upgrade/change request created for admin action.
- **Negative cases:** —
- **Code ref:** `SubscriptionScreen` → `POST /api/v1/owner/subscriptions/{venueId}/upgrade-request`.
- **Notes:** —

#### [OW-11] Profile — ✅
- **Precondition:** Owner.
- **Steps:** Owner Profile → edit/settings.
- **Expected result:** Profile + owner settings managed.
- **Negative cases:** —
- **Code ref:** `OwnerScreens.tsx` `OwnerProfileScreen` → `GET /api/v1/users/me`, `/api/v1/owner/settings`.
- **Notes:** —

#### [OW-12] Create venue coupon (owner-authored) — 🔴
- **Precondition:** Owner with a LIVE venue.
- **Steps:** (intended) Venue → Offers → create coupon → it appears on the venue card on players' Home.
- **Expected result:** Owner-scoped coupon tied to the venue, visible on that venue's card.
- **Negative cases:** N/A (unbuilt).
- **Code ref:** `CouponEntity` has **no venue/owner field**; no owner coupon endpoint; only admin `POST /api/v1/admin/coupons` + global `CouponServiceImpl.createCoupon()`.
- **Notes:** Primary product gap — needs venue/owner FK on coupons, an owner create/manage endpoint + screen, and venue-card surfacing on `PlayerHomeScreen`.

#### [OW-13] Earnings / payouts — ✅
- **Precondition:** Owner.
- **Steps:** Earnings screen.
- **Expected result:** Payout history + revenue stats.
- **Negative cases:** Owner-scoped — both endpoints key off `principal.getId()`, so one owner can't read another's payouts/stats.
- **Code ref:** `EarningsScreen` (`useOwnerPayouts`, `useOwnerStats`) → `GET /api/v1/owner/payouts` (`PayoutController.listOwnerPayouts`), `GET /api/v1/owner/stats` (`VenueController.getOwnerStats`).
- **Notes:** Verified endpoints exist, are implemented, and owner-scoped. Earlier 🟡 (inferred) was a false flag.

### Admin

#### [AD-01] Login — ✅
- **Precondition:** Admin account.
- **Steps:** Login with admin credentials.
- **Expected result:** Routed to Admin stack.
- **Negative cases:** Self-registration as ADMIN impossible (role forced).
- **Code ref:** `LoginScreen.tsx`; `AdminNavigator.tsx`.
- **Notes:** —

#### [AD-02] Add / edit / delete sports — ✅
- **Precondition:** Admin (write).
- **Steps:** Sports screen → create (name + emoji, validated) / edit / delete.
- **Expected result:** Sport list mutated; success toast; loader on submit.
- **Negative cases:** Empty name/icon → inline validation; duplicate → server error toast; READ_ONLY admin → blocked.
- **Code ref:** `AdminScreens.tsx` `CategoryManagementScreen` (`useCreateSport`/`useUpdateSport`/`useDeleteSport`) → `POST/PUT/DELETE /api/v1/admin/sports`.
- **Notes:** —

#### [AD-03] Approve venue → trial auto-trigger — ✅
- **Precondition:** Venue PENDING.
- **Steps:** Venue detail (review) → Approve.
- **Expected result:** Status → LIVE; trial subscription auto-created on approval.
- **Negative cases:** Approving non-PENDING → guarded; READ_ONLY → blocked.
- **Code ref:** `AdminVenueDetailScreen.tsx` → `PATCH /api/v1/venues/{id}/status`; trial creation in `SubscriptionServiceImpl`.
- **Notes:** —

#### [AD-04] CHANGES_REQUESTED flow — ✅
- **Precondition:** Venue PENDING.
- **Steps:** Send back with required reason.
- **Expected result:** Status → CHANGES_REQUESTED; owner notified to revise (pairs with OW-06).
- **Negative cases:** Missing reason → blocked.
- **Code ref:** `AdminVenueDetailScreen.tsx` → `PATCH /api/v1/venues/{id}/status`.
- **Notes:** —

#### [AD-05] Reject venue — ✅
- **Precondition:** Venue PENDING.
- **Steps:** Reject with optional reason.
- **Expected result:** Status → REJECTED.
- **Negative cases:** —
- **Code ref:** `AdminVenueDetailScreen.tsx` → `PATCH /api/v1/venues/{id}/status`.
- **Notes:** —

#### [AD-06] Subscriptions overview — ✅
- **Precondition:** Admin.
- **Steps:** Subscription Management → filter by status → detail.
- **Expected result:** Paginated subscriptions; edit/void/renew/activate-change.
- **Negative cases:** —
- **Code ref:** `AdminSubscriptionScreens.tsx` → `GET /api/v1/admin/subscriptions`, `/{id}`; mutations in `AdminSubscriptionController`.
- **Notes:** —

#### [AD-07] Owners list — ✅
- **Precondition:** Admin.
- **Steps:** Owners → search → detail (venues, subscriptions).
- **Expected result:** Searchable, paginated owners.
- **Negative cases:** Suspend (write) vs ban (super-admin) — see SA-02.
- **Code ref:** `AdminOwnersScreen.tsx`/`OwnerDetailScreen.tsx` → `GET /api/v1/admin/owners`, `/{id}`.
- **Notes:** —

#### [AD-08] Players / users list — ✅
- **Precondition:** Admin.
- **Steps:** Players → search → detail (bookings).
- **Expected result:** Searchable, paginated players.
- **Negative cases:** Suspend (write) vs ban/delete (super-admin).
- **Code ref:** `AdminPlayersScreen.tsx`/`PlayerDetailScreen.tsx` → `GET /api/v1/admin/players`, `/{id}`.
- **Notes:** —

#### [AD-09] Disputes triage / resolve — ✅
- **Precondition:** Admin; open disputes.
- **Steps:** Disputes → detail → assign/resolve/dismiss/reopen.
- **Expected result:** Ruling applied (no refunds; consequence/moderation model).
- **Negative cases:** READ_ONLY → blocked.
- **Code ref:** `DisputesScreen.tsx`/`DisputeDetailScreen.tsx` → `/api/v1/admin/disputes/*`.
- **Notes:** —

#### [AD-10] Coupons CRUD (admin) — 🟡
- **Precondition:** Admin.
- **Steps:** Coupon Management → create/update/list.
- **Expected result:** Global coupon created/toggled.
- **Negative cases:** Duplicate code → 409.
- **Code ref:** `AdminScreens.tsx` `CouponManagementScreen` → `/api/v1/admin/coupons`; `CouponServiceImpl`.
- **Notes:** Works, but coupons should be owner/venue-scoped per product intent (see OW-12). Keep admin tooling, migrate ownership model.

#### [AD-11] Broadcast notification — ✅
- **Precondition:** Admin.
- **Steps:** Broadcast → compose → send.
- **Expected result:** Notification broadcast.
- **Negative cases:** Empty title/body → blocked.
- **Code ref:** `AdminScreens.tsx` `NotificationBroadcastScreen` → `POST /api/v1/admin/broadcast`.
- **Notes:** —

#### [AD-12] Platform settings (super-admin gated) — ✅
- **Precondition:** Admin signed in.
- **Steps:** Profile → Platform Settings.
- **Expected result:** Visible & editable **only** to super-admin (commission/fees/toggles).
- **Negative cases:** SUPPORT/READ_ONLY admin → entry hidden **and** `GET/PUT /api/v1/admin/settings` → 403.
- **Code ref:** `AdminProfileScreen.tsx` (`adminRole==='SUPER_ADMIN'` gate); `AdminServiceImpl.getSettings/updateSettings` call `requireSuperAdmin`.
- **Notes:** UI + server both enforce.

#### [AD-13] Suspend / reactivate user — ✅
- **Precondition:** Admin with write (SUPER_ADMIN or SUPPORT).
- **Steps:** Player/Owner detail → suspend / reactivate.
- **Expected result:** Account standing changes; sessions handled.
- **Negative cases:** READ_ONLY → `requireWrite` 403.
- **Code ref:** `AdminPlayerServiceImpl.suspend` L226 / `reactivate` L245 (`requireWrite`).
- **Notes:** —

#### [AD-14] Sports form validation + feedback — ✅
- **Precondition:** Admin on Sports screen.
- **Steps:** Submit create with empty/invalid fields, then valid.
- **Expected result:** Inline field errors; loader on submit; success/error toast.
- **Negative cases:** Name < 2 chars / no icon → blocked inline.
- **Code ref:** `AdminScreens.tsx` `CategoryManagementScreen` (validate + `toast`).
- **Notes:** —

### Super-admin

#### [SA-01] All Admin capabilities — ✅
- **Precondition:** `adminRole = SUPER_ADMIN`.
- **Steps:** Perform any AD-xx operation.
- **Expected result:** Full access including write + hard-moderation.
- **Negative cases:** —
- **Code ref:** `AdminPermissionService.canWrite/canModerateHard`.
- **Notes:** Legacy NULL `adminRole` resolves to SUPER_ADMIN.

#### [SA-02] Ban user (player/owner) — ✅
- **Precondition:** Super-admin; target active.
- **Steps:** Detail → Ban (with cascade for owners).
- **Expected result:** Account BANNED; identifiers retained + locked; owner moderation cascade (unlist/archive venues, cancel+notify bookings, void subs).
- **Negative cases:** SUPPORT/READ_ONLY → `requireModerateHard` 403.
- **Code ref:** `AdminPlayerServiceImpl.ban` L263 / `AdminOwnerServiceImpl` ban.
- **Notes:** —

#### [SA-03] Unban user — ✅
- **Precondition:** Super-admin; target banned.
- **Steps:** Detail → Unban.
- **Expected result:** Account restored.
- **Negative cases:** Non-super-admin → 403.
- **Code ref:** `AdminPlayerServiceImpl.unban` L282.
- **Notes:** —

#### [SA-04] Delete user (soft-delete) — ✅
- **Precondition:** Super-admin.
- **Steps:** Detail → Delete.
- **Expected result:** Soft-delete; `active_email`/`active_phone` nulled (freed); audit fields set.
- **Negative cases:** Non-super-admin → 403.
- **Code ref:** `AdminPlayerServiceImpl.delete` L299.
- **Notes:** See B-09.

#### [SA-05] Manage admin roles (create / promote / change / remove) — ✅
- **Precondition:** Super-admin.
- **Steps:** Admin Roles → create new admin or promote existing; set SUPPORT/READ_ONLY/SUPER_ADMIN; revoke (demote) or deactivate.
- **Expected result:** Sub-role applied; `tokenVersion` bumped so it takes effect next sign-in.
- **Negative cases:** Cannot change/remove self; duplicate email on create → 409; promote non-existent/closed account → error.
- **Code ref:** `AdminRolesScreen.tsx` → `/api/v1/admin/admins`, `/promote`, `/admins/{id}`, `/users/{id}/admin-role`; `UserServiceImpl` create/promote/remove (`requireModerateHard`).
- **Notes:** —

#### [SA-06] App Configuration (diagnostics) — ✅
- **Precondition:** Super-admin.
- **Steps:** Profile → App Configuration.
- **Expected result:** Read-only runtime/config snapshot.
- **Negative cases:** Hidden for non-super-admin.
- **Code ref:** `AppConfigScreen.tsx` (gated in `AdminProfileScreen.tsx`).
- **Notes:** —

#### [SA-07] Platform Settings exclusivity — ✅
- **Precondition:** Two admins (super + support).
- **Steps:** Compare Profile menus; attempt `/admin/settings` with each token.
- **Expected result:** Super-admin sees + edits; support sees neither entry nor API.
- **Negative cases:** Support token → 403 from `requireSuperAdmin`.
- **Code ref:** `AdminProfileScreen.tsx`; `AdminServiceImpl` settings.
- **Notes:** —

#### [SA-08] Ban/delete hidden & denied for normal admin — ✅
- **Precondition:** SUPPORT/READ_ONLY admin.
- **Steps:** View detail actions; attempt ban/delete API directly.
- **Expected result:** Hard actions absent from `availableActions` (UI) **and** rejected server-side.
- **Negative cases:** Direct API call → 403.
- **Code ref:** `AdminPermissionService.filterActions` (UI) + `requireModerateHard` (server).
- **Notes:** Defense in depth — UI hiding is not the security boundary.

---

## Section B — Security audit

Server is the source of truth; client-side hiding is not security. Each item: status · risk · where · fix.

| # | Item | Status | Risk | Evidence (where) | Fix |
|---|---|---|---|---|---|
| B-01 | **JWT validation / expiry** | ✅ | — | `JwtTokenProvider.validateToken` (expiry + signature); `JwtAuthenticationFilter` checks `tokenVersion` vs DB | — |
| B-02 | **Token storage** | ✅ | — | `tokenStorage.ts` uses `expo-secure-store` on native; `localStorage` only on web (dev) | Keep native SecureStore; never ship web build as prod client |
| B-03 | **Refresh token rotation** | ✅ | — | Access vs refresh tokens are now distinct + typed (`type` claim, 30-day refresh TTL). `/auth/refresh` rejects access tokens, enforces `tokenVersion`, and rotates a new access+refresh pair each call; `JwtAuthenticationFilter` refuses refresh tokens for API auth (`JwtTokenProvider`, `AuthServiceImpl.issueTokens/refreshToken`) | Done. Future hardening: persisted refresh-token store for reuse-detection (ties to B-12) |
| B-04 | **RBAC / authZ** | ✅ | — | Class-level `@PreAuthorize("hasRole(...)")` on controllers + service-layer checks; method security enabled in `SecurityConfig` | — |
| B-05 | **IDOR / object ownership** | ✅ | — | `VenueServiceImpl` owner-id checks (submit L508, court create L851 / update L865); booking ownership in `BookingServiceImpl`; subscription ownership in service | Keep pattern on every new owner-scoped endpoint |
| B-06 | **SQL/JPA injection** | ✅ | — | `@Query` with `@Param` placeholders (`UserRepository`, `BookingRepository`); no string concatenation found | — |
| B-07 | **Input validation** | ✅ | — | Bean Validation (`@NotNull/@Email/@Size`) on DTOs + `@Valid` in controllers | — |
| B-08 | **Mass assignment** | ✅ | — | `register()` forces non-ADMIN role (`AuthServiceImpl` L106-114); `UpdateProfileRequest` has no role/plan/status/active fields | Keep DTOs minimal; never bind entities directly |
| B-09 | **Soft-delete leakage** | ✅ | — | On delete, `active_email`/`active_phone` set NULL; uniqueness on active fields (`existsByActiveEmail`); original `email` retained for audit only | Confirm list/search queries filter `status != DELETED` |
| B-10 | **OTP / login rate limiting** | ✅ | — | OTP: 6-digit SHA-256, 10-min expiry, 45 s cooldown, 5/hr, 5-attempt cap; login lock 5/15 min; change-pw 10/hr → 429 (`AuthServiceImpl`) | — |
| B-11 | **CORS** | ✅ | Med | Env-driven: `CorsConfig` reads `app.cors.allowed-origins` (`${CORS_ALLOWED_ORIGINS:*}`) — explicit list locks origins, `*`/blank stays permissive for dev + native (no Origin header) | Done — set `CORS_ALLOWED_ORIGINS` in prod to lock |
| B-12 | **Rate-limiter durability** | 🟡 (accepted) | Low | Login/OTP/change-pw **and the new write limiter (B-16)** use in-memory `ConcurrentHashMap` — reset on restart, not shared across instances | **Deliberately deferred.** At current scale (2 admins / 50 owners / 500 players on a **single instance**) in-memory counters are correct and sufficient; a restart just grants a fresh allowance (harmless). Move to Redis/DB **only before horizontal scaling to 2+ instances**, where per-instance maps would let the cap leak |
| B-13 | **Audit logging** | ✅ | Med | `admin_audit` table records actor/target/action/from→to/metadata/timestamp. Covers player+owner moderation, **and now** admin create/promote/role-change/remove + platform-settings updates | Done |
| B-14 | **Debug banner (client)** | ✅ | Low | Removed — banner JSX, unused `BASE_URL` import, and `debugBanner` style all deleted from `LoginScreen.tsx` | Done |
| B-15 | **Transport / secrets** | ✅ | — | Base URL via `EXPO_PUBLIC_API_URL`; Android cleartext gated by env in `app.json`; no server secrets in RN bundle | Enforce HTTPS-only base URL in prod config |
| B-16 | **General API rate limiting** | ✅ | — | `RateLimitInterceptor` throttles POST/PUT/PATCH/DELETE per user/IP (120/min, 60s window) → 429; registered for `/api/v1/**` in `RateLimitConfig` | Done. Durability/multi-instance tracked under B-12 |

**Strengths confirmed:** strong `tokenVersion` invalidation (password/email change, force-logout), super-admin-only ban/delete enforced in the service layer (not just annotations), parameterized queries throughout, role forced on register.

---

## Section C — API-efficiency audit

| # | Issue | Status | Current behavior | Why it matters | Fix |
|---|---|---|---|---|---|
| C-01 | **Search debounce** | ✅ | `PlayerHomeScreen` `useDebounce(query, 400)` before query params | No per-keystroke fetch storms | — |
| C-02 | **Query defaults** | ✅ | `queryClient.ts`: staleTime 30 s, `refetchOnWindowFocus:false`, smart `retry` via `classifyError`; `useSports` 10-min staleTime; infinite venues 15/page | Sensible caching, no auth/404 retries | — |
| C-03 | **Duplicate `/venues` + `/sports`** | ✅ | `useVenues` + `useInfiniteVenues` now carry `staleTime: 60_000`, so remounts (tab switches, guest→player) reuse cache instead of refetching; `useSports` already 10-min | Cuts the repeated `/venues`/`/sports` GETs seen in the network log (mount/remount churn) | Done. Optional further dedupe via shared `select` if param-fragmented keys reappear |
| C-04 | **Slot cache staleness** | ✅ | `SlotSelectionScreen` re-fetches slots on screen focus via `useFocusEffect` (skips initial mount) so returning from Booking Confirm shows live availability | Avoids booking against a stale 30s snapshot | Done |
| C-05 | **CORS preflight** | ✅ | `maxAge 3600` caches OPTIONS for an hour | Minimizes preflight overhead | — |
| C-06 | **Notification polling** | ✅ | List poll lowered/aligned to 60s with `refetchIntervalInBackground:false`; unread badge poll aligned to 60s; optimistic mark-read patches keep it live | Halves background data/battery cost | Done. Push-driven refresh is a later enhancement |

**N+1 / batching:** booking group actions use group endpoints (`/group/{groupId}/*`) rather than per-slot calls — good. No client-side N+1 loops observed.

---

## Section D — Cross-platform (Android + iOS)

| # | Item | Status | Evidence | Note |
|---|---|---|---|---|
| D-01 | Safe-area root | ✅ | `App.tsx` `SafeAreaProvider`; admin screens use `react-native-safe-area-context` + `edges` | — |
| D-02 | Mixed SafeAreaView API | ✅ | 28 player/owner/auth screens migrated to `react-native-safe-area-context` `SafeAreaView` with `edges={['top']}` (codemod) — consistent notch handling on both platforms | Done. Visual spot-check on a physical Android device recommended |
| D-03 | Keyboard avoidance | ✅ | `KeyboardAvoidingView` (iOS `padding`) wraps Login/Register/OTP/ForgotPassword inputs | Done |
| D-04 | Google Sign-In (both platforms) | 🔴 | No dependency, no iOS URL scheme/Android SHA config, no backend OAuth | Implement end-to-end or drop from scope (see PL-04) |
| D-05 | Android release signing | 🟡 (deferred — "coming soon") | `build.gradle` release signing now env/Gradle-prop driven (falls back to debug); `.gitignore` excludes keystores; runbook in `docs/RELEASE_SIGNING.md` | Deferred: create keystore + register SHA-1 (your secrets/consoles) when release/Google scheduled |
| D-06 | OTP autofill | ✅ | `OTPVerificationScreen` `textContentType="oneTimeCode"` (iOS) + `autoComplete="sms-otp"` (Android) | Both platforms |
| D-07 | Permissions | ✅ | Location `LocationContext` (`requestForegroundPermissionsAsync`); push `registerPush.ts`; `app.json` declares location/notifications/media | — |
| D-08 | Android hardware back | ✅ | New `useAndroidBack` hook (`src/hooks/useAndroidBack.ts`); ForgotPassword steps back through its flow on hardware-back instead of leaving the screen | Done. Apply the hook to other multi-step/modal screens as needed |
| D-09 | Fonts & theme / dark mode | ✅ | **Inter** bundled (`@expo-google-fonts/inter` + `expo-font`), loaded via `useFonts`, applied app-wide via a global Text/TextInput weight→family patch (`src/theme/applyFonts.ts`) — same font on iOS/Android/web. **Full dark theme**: light/dark palettes in `src/theme/index.ts` (same keys). **In-app Light/Dark/System toggle** (`AppearanceSelector` in player/owner Settings + Admin Profile) persists the choice (`store/themePreference`), applied at launch via a startup gate (`RootGate`/`index.js`) and re-applied instantly by reloading the app (`expo-updates`) | Done. Device spot-check recommended |
| D-10 | Date/time (Asia/Kolkata) | ✅ | `dateUtils.ts` hardcodes `timeZone: 'Asia/Kolkata'` (+ normalizes 9-digit fractional seconds) | — |
| D-11 | Splash / icon / scheme | ✅ | `app.json`: splash, icon, scheme `scoreadda`, portrait, bundle/package `com.scoreadda.app` | — |

---

### Appendix — verified auth endpoint map (FE ↔ BE match)
`POST /api/v1/auth/register` · `/login` · `/otp/send` · `/otp/verify` · `/forgot-password` · `/refresh` · `/password-reset/request` · `/password-reset/verify` · `/password-reset/confirm` · `/change-password`. Frontend `authService.ts` paths match backend `src/main/resources/openapi/api.yaml` and `AuthSecurityController` exactly.
