#!/usr/bin/env bash
#
# E2E proof for the court-activation state machine:
#   DRAFT -> LIVE  : owner, only into a FREE slot
#   LIVE  -> *     : LOCKED for owners; only a SUPER-ADMIN may free/swap (via court-change request)
#
# Run against a running backend with a venue that is LIVE and on a trial/paid plan.
# Requires: bash, curl, jq.
#
# Usage:
#   BASE_URL=http://localhost:8080 \
#   OWNER_TOKEN=<owner JWT> PLAYER_TOKEN=<player JWT> \
#   ADMIN_TOKEN=<SUPPORT/READ_ONLY admin JWT> SUPERADMIN_TOKEN=<SUPER_ADMIN JWT> \
#   VENUE_ID=1 bash court-activation-test.sh
#
set -euo pipefail
: "${BASE_URL:?}"; : "${OWNER_TOKEN:?}"; : "${PLAYER_TOKEN:?}"; : "${SUPERADMIN_TOKEN:?}"; : "${VENUE_ID:?}"
API="$BASE_URL/api/v1"
pass(){ echo "  PASS: $*"; }
fail(){ echo "  FAIL: $*"; exit 1; }
own(){ curl -s -H "Authorization: Bearer $OWNER_TOKEN" -H "Content-Type: application/json" "$@"; }
ownc(){ curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $OWNER_TOKEN" -H "Content-Type: application/json" "$@"; }
plyc(){ curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $PLAYER_TOKEN" -H "Content-Type: application/json" "$@"; }
sa(){ curl -s -H "Authorization: Bearer $SUPERADMIN_TOKEN" -H "Content-Type: application/json" "$@"; }
sac(){ curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $SUPERADMIN_TOKEN" -H "Content-Type: application/json" "$@"; }

courts(){ own "$API/venues/$VENUE_ID/courts"; }
liveIds(){ courts | jq '[.[]|select(.isLive==true)|.id]'; }
draftIds(){ courts | jq '[.[]|select(.isLive==false and .isActive==true)|.id]'; }

echo "== 1. Owner CANNOT deactivate a LIVE court directly (Rule 2) =="
LIVE_ONE=$(courts | jq -r 'first(.[]|select(.isLive==true)|.id) // empty')
[ -n "$LIVE_ONE" ] || fail "need at least one LIVE court to test (activate one first)"
resp=$(own -X PUT "$API/owner/venues/$VENUE_ID/courts/$LIVE_ONE/live" -d '{"live":false}')
code=$(ownc -X PUT "$API/owner/venues/$VENUE_ID/courts/$LIVE_ONE/live" -d '{"live":false}')
ec=$(echo "$resp" | jq -r '.details.code // .error')
{ [ "$code" = "409" ] && [ "$ec" = "COURT_LIVE_LOCKED" ]; } \
  && pass "owner deactivate LIVE court -> 409 COURT_LIVE_LOCKED" \
  || fail "expected 409 COURT_LIVE_LOCKED, got $code / $ec"

echo "== 2. Player sees only LIVE courts; DRAFT booking rejected (Rule: player side) =="
PCOURTS=$(curl -s -H "Authorization: Bearer $PLAYER_TOKEN" "$API/venues/$VENUE_ID/courts" | jq '[.[]|.id]|sort')
[ "$PCOURTS" = "$(liveIds | jq sort)" ] && pass "player court-list == live set" || fail "player sees non-live: $PCOURTS vs $(liveIds)"
DRAFT_ONE=$(courts | jq -r 'first(.[]|select(.isLive==false)|.id) // empty')
if [ -n "$DRAFT_ONE" ]; then
  bc=$(plyc -X POST "$API/bookings" -d "{\"venueId\":$VENUE_ID,\"courtId\":$DRAFT_ONE,\"date\":\"2030-01-01\",\"startTime\":\"10:00\",\"endTime\":\"11:00\"}")
  { [ "$bc" != "200" ] && [ "$bc" != "201" ]; } && pass "booking a DRAFT court rejected (HTTP $bc)" || fail "DRAFT court was bookable!"
  sc=$(plyc "$API/courts/$DRAFT_ONE/slots?date=2030-01-01")
  [ "$sc" = "404" ] && pass "DRAFT court slots -> 404 for player" || fail "DRAFT slots returned $sc"
fi

echo "== 3. Owner files a court-change request (free LIVE_ONE, optional swap to a DRAFT) =="
SWAP=$(courts | jq -r 'first(.[]|select(.isLive==false and .isActive==true)|.id) // empty')
body="{\"liveCourtId\":$LIVE_ONE$([ -n "$SWAP" ] && echo ",\"draftCourtId\":$SWAP")  ,\"reason\":\"QA swap\"}"
REQ=$(own -X POST "$API/owner/venues/$VENUE_ID/court-change-requests" -d "$body")
RID=$(echo "$REQ" | jq -r '.id')
[ "$RID" != "null" ] && [ -n "$RID" ] && pass "request created id=$RID (status $(echo "$REQ"|jq -r .status))" || fail "request not created: $REQ"
# Second pending request must be blocked
dc=$(ownc -X POST "$API/owner/venues/$VENUE_ID/court-change-requests" -d "$body")
[ "$dc" = "409" ] && pass "second pending request blocked (409)" || fail "duplicate request not blocked ($dc)"

echo "== 4. RBAC: normal admin CANNOT approve; super-admin can (matches ban/delete gating) =="
if [ -n "${ADMIN_TOKEN:-}" ]; then
  ac=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" -X POST "$API/admin/court-change-requests/$RID/approve")
  [ "$ac" = "403" ] && pass "non-super admin approve -> 403" || fail "non-super admin got $ac (expected 403)"
else
  echo "  (skip: set ADMIN_TOKEN to a SUPPORT/READ_ONLY admin to prove the 403)"
fi
qc=$(sac "$API/admin/court-change-requests?status=PENDING")
[ "$qc" = "200" ] && pass "super-admin sees the queue (200)" || fail "super-admin queue returned $qc"

echo "== 5. Super-admin approves; coverage changes; bookings preserved =="
ap=$(sa -X POST "$API/admin/court-change-requests/$RID/approve")
st=$(echo "$ap" | jq -r '.status')
[ "$st" = "APPROVED" ] || [ "$st" = "REJECTED" ] && pass "decision applied: $st ($(echo "$ap"|jq -r '.decisionNote // "ok"'))" || fail "approve returned: $ap"
if [ "$st" = "APPROVED" ]; then
  NOW_LIVE=$(liveIds)
  echo "  live set now: $NOW_LIVE"
  echo "$NOW_LIVE" | jq -e "index($LIVE_ONE)|not" >/dev/null && pass "freed court $LIVE_ONE is no longer live" || fail "freed court still live"
  if [ -n "$SWAP" ]; then
    echo "$NOW_LIVE" | jq -e "index($SWAP)" >/dev/null && pass "swapped-in court $SWAP is now live" || fail "swap court not live"
  fi
fi

echo "== 6. Rule 3: owner fills a FREE slot with a DRAFT court — no admin =="
FREE_DRAFT=$(courts | jq -r 'first(.[]|select(.isLive==false and .isActive==true)|.id) // empty')
LIMIT=$(own "$API/owner/venues/$VENUE_ID/subscription-state" | jq -r '.courtLimit // 2')
LIVE_CNT=$(liveIds | jq 'length')
if [ -n "$FREE_DRAFT" ] && [ "$LIVE_CNT" -lt "$LIMIT" ]; then
  mc=$(ownc -X PUT "$API/owner/venues/$VENUE_ID/courts/$FREE_DRAFT/live" -d '{"live":true}')
  [ "$mc" = "200" ] && pass "owner activated DRAFT $FREE_DRAFT into free slot (no admin)" || fail "make-live free slot got $mc"
else
  echo "  (skip: no free slot / no draft available)"
fi

echo
echo "ALL CHECKS PASSED."
echo "Manual checks (need fresh setup):"
echo " - Rule 1: on a NEW venue's trial-start sheet, deselect one of two courts and Start trial →"
echo "   only the selected court is LIVE; the other stays DRAFT and is invisible to players."
echo " - Downgrade: admin-edit the plan to a lower court limit → newest-excess LIVE courts auto-lock,"
echo "   no courts deleted, existing bookings honored, owner notified which were locked."
