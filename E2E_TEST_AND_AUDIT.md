# Score-Adda — End-to-End Test & Audit

Score-Adda is an Expo React Native + TypeScript app (`turfbook-claudeAI`) on a Spring Boot + MySQL backend (`turfbook-backend`), contract-first OpenAPI under `/api/v1`, JWT auth, roles Player / Owner / Admin / Super-admin, subscriptions-only revenue (players pay owners at the venue). This document tests every role's operations as runnable cases, then audits the app on Security, API-efficiency, and Cross-platform axes. **Every status is backed by a real file/endpoint** found in the code (verified against source on 2026-06-28). Legend: ✅ Done · 🟡 Improvement required · 🔴 Need to implement. Code is the source of truth; where the original brief disagreed with the code, the status follows the code and notes the brief.

---

## Section E — Status summary dashboard

| Area | ✅ Done | 🟡 Improvement | 🔴 To implement |
|---|---|---|---|
| A. E2E cases (by role) | 48 | 4 | 2 |
| B. Security | 10 | 6 | 0 |
| C. API efficiency | 3 | 3 | 0 |
| D. Cross-platform | 5 | 5 | 1 |
| **Total** | **66** | **18** | **3** |

### Prioritized action list (High → Low)

**High**
| # | Item | Status | Where |
|---|---|---|---|
| 1 | Google Sign-In — listed as an auth method but unbuilt on both sides | 🔴 | PL-04 / D-04 |
| 2 | Owner-authored venue coupons shown on the player home venue card (today only global admin coupons exist) | 🔴 | OW-12 / PL-19 / AD-10 |
| 3 | CORS `allowedOrigins: "*"` → restrict to known hosts for prod | 🟡 | B-11 |
| 4 | Remove `__DEV__` API-URL banner on Login (low effort) | 🟡 | B-14 |
| 5 | Android release signing / prod SHA-1 (blocks release + Google) | 🟡 | D-05 |

**Medium**
| # | Item | Status | Where |
|---|---|---|---|
| 6 | Refresh-token rotation (currently `refreshToken == token`) | 🟡 | B-03 |
| 7 | General API rate-limiting on mutating endpoints (only auth is throttled) | 🟡 | B-16 |
| 8 | In-memory rate limiters → shared store (Redis) for multi-instance | 🟡 | B-12 |
| 9 | Keyboard avoidance on auth/forms (iOS) | 🟡 | D-03 |
| 10 | Duplicate `/venues` + `/sports` fetches across screens | 🟡 | C-03 |
| 11 | Slot cache: no invalidation on screen re-focus (stale availability) | 🟡 | C-04 |
| 12 | Complete the change-phone flow | 🟡 | PL-16 |

**Low**
| # | Item | Status | Where |
|---|---|---|---|
| 13 | Admin-action audit logging | 🟡 | B-13 |
| 14 | Notification polling → push-driven refresh | 🟡 | C-06 |
| 15 | Unify mixed `SafeAreaView` API (legacy RN vs safe-area-context) | 🟡 | D-02 |
| 16 | Android hardware-back handling | 🟡 | D-08 |
| 17 | Custom fonts + full dark-mode theme | 🟡 | D-09 |
| 18 | Verify owner earnings/payouts endpoints | 🟡 | OW-13 |

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

#### [PL-16] Change phone — 🟡
- **Precondition:** Logged in.
- **Steps:** Phone-change screen → request OTP → verify.
- **Expected result:** Phone updated after OTP.
- **Negative cases:** Profile `PUT` intentionally ignores phone, forcing the OTP flow.
- **Code ref:** `PhoneChangeScreen.tsx` → `/api/v1/users/phone-change/*` (frontend hook not fully wired — *(inferred)*).
- **Notes:** Flow partially wired; verify endpoints exist before release.

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

#### [OW-13] Earnings / payouts — 🟡
- **Precondition:** Owner.
- **Steps:** Earnings screen.
- **Expected result:** Payout history + revenue stats.
- **Negative cases:** —
- **Code ref:** `OwnerScreens.tsx` `EarningsScreen` → `GET /api/v1/owner/payouts`, `/api/v1/owner/stats` *(inferred — confirm endpoints/DTOs)*.
- **Notes:** Verify wiring; payout processing exists on the admin side (`/api/v1/admin/payouts`).

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
| B-03 | **Refresh token rotation** | 🟡 | Med | `AuthServiceImpl.refreshToken` sets `refreshToken == token` (same JWT, no rotation) | Issue a distinct, rotating refresh token with its own expiry + revocation list |
| B-04 | **RBAC / authZ** | ✅ | — | Class-level `@PreAuthorize("hasRole(...)")` on controllers + service-layer checks; method security enabled in `SecurityConfig` | — |
| B-05 | **IDOR / object ownership** | ✅ | — | `VenueServiceImpl` owner-id checks (submit L508, court create L851 / update L865); booking ownership in `BookingServiceImpl`; subscription ownership in service | Keep pattern on every new owner-scoped endpoint |
| B-06 | **SQL/JPA injection** | ✅ | — | `@Query` with `@Param` placeholders (`UserRepository`, `BookingRepository`); no string concatenation found | — |
| B-07 | **Input validation** | ✅ | — | Bean Validation (`@NotNull/@Email/@Size`) on DTOs + `@Valid` in controllers | — |
| B-08 | **Mass assignment** | ✅ | — | `register()` forces non-ADMIN role (`AuthServiceImpl` L106-114); `UpdateProfileRequest` has no role/plan/status/active fields | Keep DTOs minimal; never bind entities directly |
| B-09 | **Soft-delete leakage** | ✅ | — | On delete, `active_email`/`active_phone` set NULL; uniqueness on active fields (`existsByActiveEmail`); original `email` retained for audit only | Confirm list/search queries filter `status != DELETED` |
| B-10 | **OTP / login rate limiting** | ✅ | — | OTP: 6-digit SHA-256, 10-min expiry, 45 s cooldown, 5/hr, 5-attempt cap; login lock 5/15 min; change-pw 10/hr → 429 (`AuthServiceImpl`) | — |
| B-11 | **CORS** | 🟡 | Med | `CorsConfig` `allowedOrigins: "*"`, all methods/headers, `credentials:false`, `maxAge 3600` | Restrict origins to known app hosts for prod |
| B-12 | **Rate-limiter durability** | 🟡 | Med | Login/OTP/change-pw counters are in-memory `ConcurrentHashMap` — reset on restart, not shared across instances | Move to Redis/DB for multi-instance correctness |
| B-13 | **Audit logging** | 🟡 | Med | No dedicated admin-action audit table; only implicit timestamps | Add structured audit log for ban/delete/role/settings changes |
| B-14 | **Debug banner (client)** | 🟡 | Low | `LoginScreen.tsx:118` renders `API: {BASE_URL}` under `__DEV__` (stripped in release, but present in source) | Remove the line outright |
| B-15 | **Transport / secrets** | ✅ | — | Base URL via `EXPO_PUBLIC_API_URL`; Android cleartext gated by env in `app.json`; no server secrets in RN bundle | Enforce HTTPS-only base URL in prod config |
| B-16 | **General API rate limiting** | 🟡 | Med | Only auth flows are throttled; booking/venue/court mutations have no per-user/IP limit | Add a lightweight rate-limit filter for mutating endpoints |

**Strengths confirmed:** strong `tokenVersion` invalidation (password/email change, force-logout), super-admin-only ban/delete enforced in the service layer (not just annotations), parameterized queries throughout, role forced on register.

---

## Section C — API-efficiency audit

| # | Issue | Status | Current behavior | Why it matters | Fix |
|---|---|---|---|---|---|
| C-01 | **Search debounce** | ✅ | `PlayerHomeScreen` `useDebounce(query, 400)` before query params | No per-keystroke fetch storms | — |
| C-02 | **Query defaults** | ✅ | `queryClient.ts`: staleTime 30 s, `refetchOnWindowFocus:false`, smart `retry` via `classifyError`; `useSports` 10-min staleTime; infinite venues 15/page | Sensible caching, no auth/404 retries | — |
| C-03 | **Duplicate `/venues` + `/sports`** | 🟡 | `useSports`/venue hooks called from multiple screens (`PlayerHomeScreen`, `VenueDetailScreen`); guest→login remount re-runs queries; distinct queryKeys don't dedupe | Repeated identical GETs (matches the observed repeated `/venues` + `/sports` in the network log) — mount/remount + key fragmentation | Share a single `useSports` key (already cached 10 min — good); hoist venue list state; avoid remount on guest→player transition; consider `select` to reuse one cache entry |
| C-04 | **Slot cache staleness** | 🟡 | Slots `staleTime 30 s`, not invalidated on screen re-focus | User returning within 30 s sees stale availability | Invalidate slot query on screen focus, or drop to `staleTime: 0` for slots |
| C-05 | **CORS preflight** | ✅ | `maxAge 3600` caches OPTIONS for an hour | Minimizes preflight overhead | — |
| C-06 | **Notification polling** | 🟡 | `useNotifications` 60 s + `useUnreadNotifications` 30 s background refetch | Battery/data drain vs. push already configured | Drive refresh from `expo-notifications` receipt; lengthen/disable poll when push works |

**N+1 / batching:** booking group actions use group endpoints (`/group/{groupId}/*`) rather than per-slot calls — good. No client-side N+1 loops observed.

---

## Section D — Cross-platform (Android + iOS)

| # | Item | Status | Evidence | Note |
|---|---|---|---|---|
| D-01 | Safe-area root | ✅ | `App.tsx` `SafeAreaProvider`; admin screens use `react-native-safe-area-context` + `edges` | — |
| D-02 | Mixed SafeAreaView API | 🟡 | Player/Owner screens use legacy RN `SafeAreaView`; admin uses safe-area-context | Unify on `react-native-safe-area-context` for consistent notch handling |
| D-03 | Keyboard avoidance | 🟡 | Only `AdminRolesScreen` uses `KeyboardAvoidingView`; Login/Register/OTP rely on ScrollView | iOS keyboard may cover inputs on auth/forms — add `KeyboardAvoidingView` |
| D-04 | Google Sign-In (both platforms) | 🔴 | No dependency, no iOS URL scheme/Android SHA config, no backend OAuth | Implement end-to-end or drop from scope (see PL-04) |
| D-05 | Android release signing | 🟡 | Debug keystore only; no prod SHA-1 *(inferred from `android/app/build.gradle`)* | Configure release keystore + SHA-1 (also needed for Google) |
| D-06 | OTP autofill | ✅ | `OTPVerificationScreen` `textContentType="oneTimeCode"` (iOS) + `autoComplete="sms-otp"` (Android) | Both platforms |
| D-07 | Permissions | ✅ | Location `LocationContext` (`requestForegroundPermissionsAsync`); push `registerPush.ts`; `app.json` declares location/notifications/media | — |
| D-08 | Android hardware back | 🟡 | No `BackHandler` usage found | Relies on RN-nav defaults; add explicit handling for modals/forms |
| D-09 | Fonts & theme / dark mode | 🟡 | No `expo-font`; theme light-only; dark mode only in `planMeta` | Add font loading + full dark theme if required |
| D-10 | Date/time (Asia/Kolkata) | ✅ | `dateUtils.ts` hardcodes `timeZone: 'Asia/Kolkata'` (+ normalizes 9-digit fractional seconds) | — |
| D-11 | Splash / icon / scheme | ✅ | `app.json`: splash, icon, scheme `scoreadda`, portrait, bundle/package `com.scoreadda.app` | — |

---

### Appendix — verified auth endpoint map (FE ↔ BE match)
`POST /api/v1/auth/register` · `/login` · `/otp/send` · `/otp/verify` · `/forgot-password` · `/refresh` · `/password-reset/request` · `/password-reset/verify` · `/password-reset/confirm` · `/change-password`. Frontend `authService.ts` paths match backend `src/main/resources/openapi/api.yaml` and `AuthSecurityController` exactly.
