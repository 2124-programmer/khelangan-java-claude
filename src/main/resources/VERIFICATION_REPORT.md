# Score‑Adda / TurfBook — Verification Report

> Verification + documentation pass run on 2026‑06‑26. No feature behavior was changed.
> Legend: ✅ holds · ⚠️ partial / divergence · ❌ fails / missing.
> Paths relative to repo root. Backend `turfbook-backend/src/main/java/com/turfbook/backend`,
> frontend `turfbook-claudeAI/src`. The subscription flow is documented separately in
> [`SUBSCRIPTION_FLOW.md`](SUBSCRIPTION_FLOW.md).

---

## Executive summary

| Area | Verdict |
|---|---|
| Frontend `tsc --noEmit` | ✅ passes (exit 0) |
| Frontend "no `any`" requirement | ❌ 129 explicit `any` across 52 files |
| Backend main compile + package (jar) | ✅ builds (`turfbook-backend-1.0.0-SNAPSHOT.jar`) |
| Backend test suite | ❌ broken — fails to compile **and** run (5/5 error) |
| OpenAPI spec validity | ✅ valid (generator parses it during build) |
| OpenAPI ↔ backend DTO sync | ✅ contract‑first (generated at build) |
| OpenAPI ↔ frontend client sync | ⚠️ frontend client is hand‑written, not generated → unverifiable drift |
| Core monetization gate (court coverage) | ✅ enforced server‑side (verified directly) |
| Soft‑delete identity model | ⚠️ holds for Owners; no Player delete |
| RBAC sub‑roles (SUPER_ADMIN/SUPPORT/READ_ONLY) | ❌ not implemented (stubs hardcoded `true`) |
| `__DEV__`‑gated API debug logging | ❌ API logs are unconditional (ship in release) |
| Push notifications (FCM/APNs) | ❌ entirely absent |
| Production API URL | ❌ falls back to plain HTTP LAN IP |

No build fixes were applied. The backend test breakage is pre‑existing and out of scope for a
"trivial build fix" (see Part 1.B); fixing it would mean rewriting stale test sources, which risks
changing behavior.

---

## Part 1 — Build & static verification

### 1.A Frontend — ✅ tsc / ❌ no‑`any`
- `npx tsc --noEmit` → **exit 0, no errors.** ✅
- **`any` requirement ❌:** 129 explicit `any` occurrences across 52 files. Most are RN navigation
  props `({ navigation, route }: any)` (e.g. `screens/auth/LoginScreen.tsx:20`,
  `screens/player/MiscScreens.tsx:21`), but several are data‑shaped and should be typed:
  - `components/venue/FilterModal.tsx:48` `applyFilters(venues: any[], f): any[]`
  - `api/hooks/usePlayers.ts:52,68` and `api/hooks/useOwners.ts:82,98` `(d: any) => any`, `(last: any)`
  - `screens/admin/AdminScreens.tsx` (17), `OwnerScreens.tsx` (9), `OwnerDetailScreen.tsx` (11),
    `PlayerDetailScreen.tsx` (10), `DisputeDetailScreen.tsx` (7), `MiscScreens.tsx` (8).
  `tsc` passes because explicit `any` is always allowed; the project does **not** enforce
  `no-explicit-any`. There is **no lint script** in `package.json` (no ESLint config present), so the
  "run the linter" step could not be executed. ⚠️
- Expo config: static `app.json` loads (no `app.config.js`). ✅

### 1.B Backend — ✅ main build / ❌ tests
- **`mvn -Dmaven.test.skip=true clean package` → exit 0**, produces
  `target/turfbook-backend-1.0.0-SNAPSHOT.jar` (~67 MB). Main code compiles cleanly. ✅
- **`mvn clean package` (runs tests) → BUILD FAILURE.** `Tests run: 5, Errors: 5`. The test
  module is broken on two levels:
  1. **Test compilation fails** (surfaced via `-DskipTests`): `src/test/.../venue/VenueGoLiveFlowTest.java`
     reports `package com.turfbook.backend.entity does not exist` (and `exception`, `repository`) at
     lines 13‑20 — stale test sources whose classpath doesn't resolve.
  2. **Test execution errors** (full run): `NoClassDefFoundError: com.turfbook.backend.service.subscription.SubscriptionService`
     (`SubscriptionServiceTest`), `NoClassDefFoundError: com.turfbook.backend.service.VenueService`
     (`VenueGoLiveFlowTest`), `Unable to find a @SpringBootConfiguration` (`TurfBookApplicationTests`),
     and `Failed to load ApplicationContext` (`BookingNotificationDismissTest`).
  Tests present: `TurfBookApplicationTests`, `BookingServiceTest`, `SubscriptionServiceTest`,
  `VenueGoLiveFlowTest`, `BookingNotificationDismissTest`. **None currently pass.** ❌
  > Severity **High** for CI hygiene, but does not affect the shippable app jar. Left unfixed by
  > design (non‑trivial; out of scope for this pass).

### 1.C OpenAPI — ✅ valid / ✅ backend sync / ⚠️ frontend sync
- `api.yaml` is OpenAPI **3.0.3**, 5742 lines; it is consumed by the
  `openapi-generator-maven-plugin` 7.6.0 during the build (`pom.xml:189`), which **parsed it
  successfully** (build reached packaging) — strong validity signal. A standalone linter (Spectral)
  is not installed, so style‑lint was not run. ✅ valid
- **Backend = contract‑first:** DTOs/server stubs are generated from `api.yaml` at build time, so
  backend ↔ contract cannot drift by construction. ✅
- **Frontend client is NOT generated.** `package.json:11` has a `generate-api` script
  (typescript‑axios → `src/api/generated`) but **no `src/api/generated/` directory exists** — the
  app uses hand‑written `src/api/client.ts`, `types.ts`, `adapters.ts`, and `services/`. Therefore
  frontend types can silently drift from the contract; this is not tool‑verified. ⚠️ **Medium.**

---

## Part 2 — Cross‑cutting invariants

### #1 Subscriptions‑only / platform holds no booking money — ✅
No booking‑refund / money‑movement code exists; there is no payment gateway integration anywhere.
- `service/impl/BookingServiceImpl.java:349,432` only flips a `paymentStatus` enum to `REFUNDED`
  (a column, no gateway call); enum at `entity/BookingEntity.java:26`.
- Disputes are explicitly no‑refund and owner‑settled: `service/impl/AdminDisputeServiceImpl.java:284-291`
  ("refund directly. Score‑Adda does not process this payment"); `setRecommendedRefundAmount` (`:275`)
  is informational only.
- Cascade comments confirm "platform never refunds, payments are direct owner↔player":
  `AdminOwnerServiceImpl.java:54,497`.
- Frontend refund language is copy only (FAQ `screens/player/MiscScreens.tsx:92,103-104`).
- Dashboard hero is **MRR / active subscriptions**, not booking GBV (`AdminDashboardScreen.tsx:41,72-73`).

### #2 Plan tiers + court limits (2/2/4/6/12, ₹0/499/899/1299/1999) — ⚠️
Values match, single source of truth is `bootstrap/SubscriptionPlanSeeder.java:34-43`. **Divergence:**
"Trial" is **not a real plan tier** — only 4 `PlanCode`s exist (STARTER/GROWTH/PRO/PRO_MAX =
2/4/6/12); the trial is a TRIALING‑status subscription on STARTER capped to `TRIAL_COURT_LIMIT = 2`
(`SubscriptionServiceImpl.java:95,656`). The limit is treated as a max (owner may cover fewer).
Full detail in [`SUBSCRIPTION_FLOW.md` §2](SUBSCRIPTION_FLOW.md). ⚠️ **Low** (cosmetic/structural).

### #3 Court‑coverage gating, server‑side discovery filter — ✅ (verified directly)
A court is bookable only if covered by a TRIALING/ACTIVE subscription on a LIVE venue; venues with
0 bookable courts are filtered out **server‑side at the JPQL level**:
- `repository/VenueRepository.java:41` and `:59`:
  `v.status = :liveStatus AND v.subscriptionActive = true AND v.bookableCourtCount > 0`.
- Booking‑time second gate: `BookingServiceImpl.java:143,607` → `subscriptionGate.isCourtBookable`.
- Flags maintained by `recomputeVenueLive` (`SubscriptionServiceImpl.java:923-953`).
This is the strongest‑implemented invariant. ✅

### #4 Soft‑delete identity model — ⚠️
- `active_email`/`active_phone` fields: `entity/UserEntity.java:60-65`; raw email/phone deliberately
  non‑unique & retained (`:43-52`). ✅
- Unique indexes on `active_email`/`active_phone` are created at runtime by
  `bootstrap/ActiveIdentifierMigration.java:46-47,106-114` (`CREATE UNIQUE INDEX`), **not** via JPA
  annotations — MySQL multi‑NULL semantics let a DELETED row (active_*=NULL) coexist with a fresh
  claim. ✅ (internally consistent, but the constraint lives in a migration runner, not the schema.)
- Delete nulls identifiers: `AdminOwnerServiceImpl.java:510-519` (status=DELETED, blocked, active_*
  set null, row kept). ✅
- Ban retains identifiers: Player `AdminPlayerServiceImpl.java:259-263`, Owner `AdminOwnerServiceImpl.java:381-385`. ✅
- Uniqueness checks use `active_*`: registration `AuthServiceImpl.java:99,102`; email change
  `EmailChangeServiceImpl.java:65,159`. ✅
- **Gap ❌:** there is **no Player delete** — `AdminPlayerServiceImpl` omits delete by design
  (`:464` comment, `:469`). Only Owner delete exists. ⚠️ **Medium** (matches known "Players DELETE pending").

### #5 Server‑authoritative `availableActions` — ⚠️
Backend builds the action arrays server‑side and the frontend renders from them (not client guesses):
- Build sites: Players `AdminPlayerServiceImpl.java:463-471`; Owners `AdminOwnerServiceImpl.java:718-726`;
  Disputes `AdminDisputeServiceImpl.java:526-533`; Venues `VenueServiceImpl.java:730-738`.
- Frontend consumes verbatim: `screens/admin/AdminVenueDetailScreen.tsx:261-263`,
  `PlayerDetailScreen.tsx:84-86`, `OwnerDetailScreen.tsx:112-113`, `DisputeDetailScreen.tsx:69`.
- **Gap ❌:** arrays are computed from **status only**, **not role‑filtered**. No method takes a
  caller role; the Owner‑has‑DELETE / Player‑no‑DELETE difference is hardcoded per entity. ⚠️ **Medium.**

### #6 RBAC (SUPER_ADMIN/SUPPORT/READ_ONLY; ban+delete super‑admin only) — ❌
Not implemented. Only `Role { PLAYER, OWNER, ADMIN }` exists (`entity/UserEntity.java:22-24`); the
sub‑roles appear only in comments. The super‑admin gates are **no‑op stubs hardcoded to allow
everyone**:
- `AdminPlayerServiceImpl.requireModerateHard:481-484` → `boolean canModerateHard = true;`
- `AdminOwnerServiceImpl.requireModerateHard:746-749` → `true`
- `AdminDisputeServiceImpl.requireCanBan:404-408` → `true`
Admin controllers are class‑gated `@PreAuthorize("hasRole('ADMIN')")` only. **Net: any authenticated
ADMIN can ban and delete.** ❌ **High** (security expectation unmet).

### #7 Audit logging — ⚠️
- Moderation + deletes are audited via `entity/AdminAuditEntity.java` (append‑only) and private
  `audit(...)` helpers: Player BAN `AdminPlayerServiceImpl.java:264`; Owner SUSPEND
  `AdminOwnerServiceImpl.java:339-340`; Owner DELETE `:525-527`; Dispute `AdminDisputeServiceImpl.java:367,371`. ✅
- **Divergence:** subscription activation is **not** written to `admin_audit` — it records to a
  parallel `VenueLifecycleEventEntity` trail via `recordLifecycle(...)`
  (`SubscriptionServiceImpl.java:422,454,1081-1093`). Both are audited, via two independent
  mechanisms (no unified audit service). ⚠️ **Low.**

### #8 Time + UX conventions — ✅ (mostly)
- `Asia/Kolkata` via `ZoneId.of("Asia/Kolkata")` in the date‑math classes
  (`SubscriptionDateCalculator.java:16`, `AdminDashboardServiceImpl.java:35`,
  `OwnerDashboardServiceImpl.java:34`). Note: no JVM‑wide default; JDBC `serverTimezone=UTC`
  (`application.yml:3`) — IST is applied explicitly per‑calculation. ✅
- Frontend `timeAgo` = `formatRelativeTime` (`utils/dateUtils.ts:25-51`) with a `useNow()` ticker. ✅
- Toast on every mutation: global `MutationCache.onError` (`api/queryClient.ts:17-22`) + per‑mutation
  toasts; suppressible via `meta.suppressToast`. ✅
- Typed query keys: present and namespaced, but as `as const` tuple constants
  (`ADMIN_PLAYERS_KEY`, `OWNER_VENUES_KEY`, `ADMIN_SUB_KEY`…), **not** `adminKeys/ownerKeys/playerKeys`
  factory objects (`api/hooks/usePlayers.ts:13`, `useSubscription.ts:16-20`). Intent met, naming
  differs. ⚠️ **Low.**

---

## Part 3 — Feature‑by‑feature

### Auth
| Item | Verdict | Evidence |
|---|---|---|
| Generic credential error | ✅ | `AuthServiceImpl.java:161`; FE `LoginScreen.tsx:60-61` |
| Account‑state handling (banned/deleted) | ✅ | `AuthServiceImpl.java:153-171,498-509` |
| Secure‑store tokens | ✅ | `store/AuthContext.tsx:92-93,132-133`; `api/tokenStorage.ts:15-18` |
| Post‑auth redirect by role | ✅ | `navigation/RootNavigator.tsx:10-21` |
| Register `active_*` uniqueness + friendly errors | ✅ | `AuthServiceImpl.java:99-104`; FE `RegisterScreen.tsx:95-104` |
| Live password checklist | ✅ | `RegisterScreen.tsx:35-39,199-249` |
| Terms consent | ✅ | FE `RegisterScreen.tsx:30,54`; BE `AuthServiceImpl.java:91-93,125` |
| Forgot‑pw email OTP (sender, hash, TTL, attempts, cooldown) | ✅ | `MailServiceImpl.java:121`; `AuthServiceImpl.java:376-427` (hash `:392`, TTL 600s, max 5, cooldown 45s) |
| Enumeration‑safe reset | ✅ | identical response all paths `AuthServiceImpl.java:354-406` |
| Session invalidation on reset | ✅ | `tokenVersion++` `:486`; `JwtAuthenticationFilter.java:48-64` |
| **`__DEV__` API debug line not in release** | ❌ | **No `__DEV__` guard on API logging.** `api/client.ts:44,69,78-81` logs every request/response/error unconditionally. **High.** |

Minor: FE 403 copy hardcoded "suspended" ignores specific backend message (`LoginScreen.tsx:62-63`);
stale OTP comment `api/types.ts:31-32`.

### Player home / discovery — ✅ all
Location→distance/nearest sort + denied fallback strip (`LocationContext.tsx:81-115`,
`PlayerHomeScreen.tsx:93-108,199-208`); search + sport chips not clipped (`:170-192`); FilterModal
server‑applied; venue cards with sport icons/distance/graceful rating (`components/venue/index.tsx`,
`components/reviews/index.tsx:55-65`); bookable‑court gate is server‑side and the client relies on it.

### Booking + contact
| Item | Verdict | Evidence |
|---|---|---|
| Guest Book Now → auth gate → return | ✅ | `VenueDetailScreen.tsx:684-698`; `utils/pendingNav.ts:3-19`; `PlayerHomeScreen.tsx:52-67` |
| Phone hidden from guests (server‑nulled) | ✅ | BE `VenueServiceImpl.java:235-238`; FE `VenueDetailScreen.tsx:522,650` |
| Call / WhatsApp launch | ✅ | `modals/index.tsx:149,156` (`tel:`, `wa.me`) |
| Owner notified, deduped | ⚠️ | dedup is a **30‑min cooldown** per (player,venue), not once‑ever (`VenueServiceImpl.java:74,176-201`) — flag if "once forever" intended. **Low.** |
| Best‑effort (launches even if notify fails) | ✅ | non‑blocking `.mutate` `VenueDetailScreen.tsx:653`; errors swallowed `useVenues.ts:195-197` |

### Player profile
| Item | Verdict | Evidence |
|---|---|---|
| Edit profile + avatar upload | ✅ | `MiscScreens.tsx:240-255`; `api/services/userService.ts:12-29`; BE `UserAvatarController.java:52-80` |
| **Phone‑change OTP + uniqueness** | ❌ | plain text field, no OTP, no uniqueness, never updates `active_phone` (`UserServiceImpl.java:51-53`). **Medium.** |
| Change‑email (verify → apply) | ✅ | `EmailChangeServiceImpl.java:154-185` (verify‑OTP‑only; uniqueness on `active_email`). Dead "admin review" copy `EmailChangeScreen.tsx:26-30`. |
| **Settings toggles persisted** | ❌ | player toggles are local `useState`, never saved (`MiscScreens.tsx:178-204`); `settingsService` is owner‑only. **Medium.** |
| **Delete account (soft‑delete)** | ❌ | no player‑facing delete exists; soft‑delete is admin‑only. **Medium.** |
| Help: owner‑settled refund copy | ✅ | `MiscScreens.tsx:104` |
| Offers: empty state | ⚠️ | Coupons live (`useCoupons`) but zero‑coupon renders blank, no placeholder (`MiscScreens.tsx:43`). **Low.** |

### Owner — ✅
Dashboard (`OwnerDashboardScreen.tsx`), My Venues (`MyVenuesScreen.tsx`), subscription purchase
(Trial instant vs paid→request, `components/subscription/OwnerSubscriptionPurchase.tsx:297-312`),
court selection capped to plan limit with counter + upgrade deep‑link
(`OwnerSubscriptionPurchase.tsx:286-295,409-413`; `CourtManagementScreen.tsx:158-167`), and
player‑visibility coverage strip (`:79-99,515-536`).

### Admin — ✅ / ⚠️
Dashboard MRR/active‑subs hero (`AdminDashboardScreen.tsx:41,72-73`); all sibling screens
(Venues/Players/Owners/Disputes/Notifications/Subscriptions/Profile) wired in
`navigation/AdminNavigator.tsx`; subscriptions queue → activate/renew/change/suspend
(`AdminSubscriptionScreens.tsx:291-298,370-487`); moderation via server `availableActions`
(see invariant #5). ⚠️ READ_ONLY action‑hiding not demonstrable because the role doesn't exist
server‑side (invariant #6).

### Notifications
| Item | Verdict | Evidence |
|---|---|---|
| In‑app feed + mark‑read | ✅ | `NotificationsScreen.tsx:27,56-73`; `NotificationController.java:26-51` |
| **Push (expo‑notifications)** | ❌ | no `expo-notifications` dep, no token registration, no backend push send — **polling only**. **High** for a mobile app. |
| **Preferences respected** | ❌ | `pushNotificationsEnabled` stored (`OwnerSettingsServiceImpl.java:48-49,75`) but never read at send time; owner‑only. **Medium.** |
| Bell on headers | ⚠️ | present only on the two dashboards (`OwnerDashboardScreen.tsx:129`, `AdminDashboardScreen.tsx:58`); admin list/detail headers have none. **Low.** |

---

## Part 4 — Cross‑platform config

App identity (`turfbook-claudeAI/app.json`): name `Score‑Adda` (`:3`), slug `score-adda` (`:4`),
version `1.0.0` (`:6`), scheme `scoreadda` (`:5`). Static `app.json` (no `app.config.js`). There is a
committed `android/` prebuild but **no `ios/` folder**.

### iOS
| Check | Verdict | Evidence |
|---|---|---|
| Location usage string | ✅ | `app.json:22-27` (expo-location plugin) |
| Photo library usage string | ✅ | `app.json:28-33` (expo-image-picker) |
| **Camera usage string** | ❌ | no `NSCameraUsageDescription` / `cameraPermission`. **Medium.** |
| **`LSApplicationQueriesSchemes` incl. `whatsapp`** | ⚠️ | absent; mitigated because app uses universal `https://wa.me/...` not `whatsapp://` (`modals/index.tsx:156`), so `canOpenURL` isn't needed. **Low.** |
| **App Transport Security (no plain HTTP in prod)** | ❌ | no `NSAppTransportSecurity`; default API base is `http://192.168.1.50:8080` (`api/client.ts:7,11`, `.env:1`). **High.** |
| `ios.bundleIdentifier == com.scoreadda.app` | ✅ | `app.json:45` |
| `ios.buildNumber` set | ✅ | `app.json:46` (`"1"`) |

### Android
| Check | Verdict | Evidence |
|---|---|---|
| **`POST_NOTIFICATIONS` (API 33+)** | ❌ | not in `app.json:55-58` nor `AndroidManifest.xml`. **High** (with push absent). |
| Location (FINE/COARSE) | ✅ | `app.json:56-57` |
| Media/storage | ⚠️ | none in app.json; stale manifest has legacy `READ/WRITE_EXTERNAL_STORAGE` only, no `READ_MEDIA_IMAGES`. **Low** (expo-image-picker auto‑adds on prebuild). |
| `android.package == com.scoreadda.app` | ⚠️ | `app.json:49` ✅ **but** stale native `android/app/build.gradle:112,114` = `com.turfbook.app`, and manifest deep‑link schemes still `exp+turfbook` (`:31-32`). **Medium** — wrong package ships if built from committed `android/`. |
| `android.versionCode` set | ✅ | `app.json:50` (`1`) |
| **FCM configured** | ❌ | no `google-services.json`, no `googleServicesFile`, no `expo-notifications` plugin/dep. **High.** |

### Both / EAS
| Check | Verdict | Evidence |
|---|---|---|
| EAS Android APK + AAB | ✅ | `eas.json` preview/uat→apk (`:8-22`), production→app-bundle (`:24-28`) |
| **EAS iOS `.ipa`** | ❌ | no `ios` block in any profile (`eas.json:1-33`); no `ios/` folder. **Medium.** |
| Push (APNs + FCM) | ❌ | neither configured. **High.** |
| expo‑secure‑store | ✅ | `package.json:31`; `api/tokenStorage.ts` |
| Google Sign‑In per‑platform | ❌ | no library; only empty env placeholders `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID=` (`.env:3`). Feature does not exist. **Low** (if not required for v1). |
| Safe areas | ✅ | `react-native-safe-area-context` in `App.tsx:4,62,73`, used app‑wide |
| **API base URL https / `__DEV__`‑gated logs** | ❌ | default = http; UAT = https (`.env.uat:1`); **production `eas.json` sets no `EXPO_PUBLIC_API_URL`** → prod build falls back to the HTTP LAN IP (`client.ts:7,11`). Logs at `client.ts:44,69,78,109,114` not `__DEV__`‑gated. `usesCleartextTraffic:true` forced (`app.json:38`). **Critical.** |

---

## Prioritized recommended fixes

### Critical
1. **Production API URL falls back to plain HTTP LAN IP.** Set `EXPO_PUBLIC_API_URL` to the HTTPS
   production endpoint in the `production` profile of `eas.json` (mirror `.env.uat:1`), and remove
   `usesCleartextTraffic:true` for production (`app.json:38`). Without this, prod builds are
   insecure/broken and iOS ATS blocks them. (Part 4)
2. **API debug logging ships in release.** Wrap `api/client.ts:44,69,78,109,114` in `if (__DEV__)`.
   (Part 1.A / 3 Auth / 4)

### High
3. **Backend test suite is broken** (compile + run). Repair stale imports in
   `VenueGoLiveFlowTest`/`SubscriptionServiceTest` (point at the real `entity`/`service.subscription`
   packages), add a `@SpringBootTest`/test datasource (H2) config so the context loads, then green
   the suite in CI. (Part 1.B)
4. **RBAC sub‑roles unimplemented; any ADMIN can ban/delete.** Introduce SUPER_ADMIN/SUPPORT/READ_ONLY
   and replace the hardcoded `true` guards (`AdminPlayerServiceImpl.java:481`,
   `AdminOwnerServiceImpl.java:746`, `AdminDisputeServiceImpl.java:404`) with real role checks; gate
   ban/delete to super‑admin. (Invariant #6)
5. **Push notifications absent.** Add `expo-notifications` + FCM (`google-services.json`,
   `googleServicesFile`, `POST_NOTIFICATIONS`) + APNs, backend push‑token storage/send, and honor the
   stored `pushNotificationsEnabled` preference. (Part 3E / Part 4)

### Medium
6. **No iOS build path** — add an `ios` profile to `eas.json` and prebuild the `ios/` project. (Part 4)
7. **Stale native android prebuild** — regenerate `android/` so `applicationId`/namespace =
   `com.scoreadda.app` and deep‑link scheme = `scoreadda` (currently `com.turfbook.app`). (Part 4)
8. **Player profile gaps** — phone‑change OTP + `active_phone` uniqueness (`UserServiceImpl.java:51-53`);
   persist player settings toggles; add player self‑service soft‑delete‑account. (Part 3)
9. **`availableActions` not role‑filtered** — once RBAC exists, filter the arrays by caller role.
   (Invariant #5)
10. **Player DELETE endpoint** still pending (`AdminPlayerServiceImpl`). (Invariant #4)
11. **Add iOS camera usage string** and (defensively) `LSApplicationQueriesSchemes: [whatsapp]`. (Part 4)
12. **Frontend client drift risk** — either run/commit the generated client or add a CI check that
    diffs `api/types.ts` against `api.yaml`. (Part 1.C)

### Low
13. Reduce explicit `any` in data layers (`FilterModal.tsx:48`, `usePlayers.ts`, `useOwners.ts`,
    admin detail screens) and add an ESLint config with `no-explicit-any`. (Part 1.A)
14. Add an Offers empty‑state placeholder (`MiscScreens.tsx:43`); fix dead "admin review" email‑change
    copy and stale OTP comments; add the notification bell to more headers; confirm contact‑notify
    dedup window (30‑min vs once‑ever) is intended. (Part 3)

---

## Build fixes applied
**None.** This was a verification + documentation pass only. The backend test breakage is
pre‑existing and was deliberately left unfixed (non‑trivial; would require rewriting stale test
sources and adding test infrastructure — outside "trivial build fix" scope). The shippable
application jar builds cleanly with `mvn -Dmaven.test.skip=true package`.
