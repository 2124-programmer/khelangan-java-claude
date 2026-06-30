#!/usr/bin/env bash
#
# E2E proof for: "courts are unlimited to create, but only the plan's quota can be LIVE
# (player-bookable)". Run against a running backend with a venue that is already LIVE and on
# a trial/paid plan (e.g. the 'Magic global Turf' from the screenshots).
#
# Requires: bash, curl, jq.
#
# Usage:
#   BASE_URL=https://khelangan-java-claude-uat.up.railway.app \
#   OWNER_TOKEN=<owner JWT> PLAYER_TOKEN=<player JWT> VENUE_ID=1 \
#   bash court-live-quota-test.sh
#
# What it proves (maps to the acceptance criteria):
#   1. Create 4 courts on a 2-court plan -> ALL created (201), no COURT_LIMIT error.
#   2. Owner sees all courts (live + locked); exactly <=limit are live.
#   3. Owner can toggle locked->live up to the limit; the (limit+1)th is a SINGLE 409 COURT_LIVE_LIMIT.
#   4. Player venue-details + court-list + slot-list return ONLY live courts.
#   5. Player booking a LOCKED court id is rejected server-side (not 2xx).
#   6. Disabling a live court always succeeds (frees a slot) and is idempotent.
#
set -euo pipefail

: "${BASE_URL:?set BASE_URL}"; : "${OWNER_TOKEN:?set OWNER_TOKEN}"
: "${PLAYER_TOKEN:?set PLAYER_TOKEN}"; : "${VENUE_ID:?set VENUE_ID}"
API="$BASE_URL/api/v1"
pass(){ echo "  PASS: $*"; }
fail(){ echo "  FAIL: $*"; exit 1; }
owner(){ curl -s -H "Authorization: Bearer $OWNER_TOKEN" -H "Content-Type: application/json" "$@"; }
player(){ curl -s -H "Authorization: Bearer $PLAYER_TOKEN" -H "Content-Type: application/json" "$@"; }
# Print HTTP status only
ostat(){ curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $OWNER_TOKEN" -H "Content-Type: application/json" "$@"; }
pstat(){ curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $PLAYER_TOKEN" -H "Content-Type: application/json" "$@"; }

echo "== 0. Read plan limit & first sport =="
STATE=$(owner "$API/owner/venues/$VENUE_ID/subscription-state")
LIMIT=$(echo "$STATE" | jq -r '.courtLimit // 2')
SPORT=$(owner "$API/venues/$VENUE_ID/courts" | jq -r '.[0].sportId // 1')
echo "  plan live-court limit = $LIMIT, sport = $SPORT"

echo "== 1. Create 4 courts (should all succeed even on a $LIMIT-court plan) =="
for i in 1 2 3 4; do
  code=$(ostat -X POST "$API/venues/$VENUE_ID/courts" \
    -d "{\"name\":\"QA Court $i $(date +%s)$i\",\"sportId\":$SPORT,\"isActive\":true}")
  [ "$code" = "201" ] && pass "court $i created (201)" || fail "court $i create returned $code (expected 201, NOT a limit block)"
done

echo "== 2. Owner sees all courts; bounded live count =="
COURTS=$(owner "$API/venues/$VENUE_ID/courts")
TOTAL=$(echo "$COURTS" | jq 'length')
LIVE=$(echo "$COURTS" | jq '[.[]|select(.isLive==true)]|length')
echo "  total=$TOTAL live=$LIVE limit=$LIMIT"
[ "$TOTAL" -ge 4 ] && pass "owner sees $TOTAL courts (incl. locked)" || fail "owner should see >=4 courts"
[ "$LIVE" -le "$LIMIT" ] && pass "live count $LIVE <= limit $LIMIT" || fail "live count exceeds limit"

echo "== 3. Toggle locked->live up to the limit, then expect ONE 409 =="
LOCKED_IDS=$(echo "$COURTS" | jq -r '[.[]|select(.isLive==false and .isActive==true)|.id]|@sh' | tr -d "'")
i=$LIVE
for cid in $LOCKED_IDS; do
  if [ "$i" -lt "$LIMIT" ]; then
    code=$(ostat -X PUT "$API/owner/venues/$VENUE_ID/courts/$cid/live" -d '{"live":true}')
    [ "$code" = "200" ] && { pass "court $cid -> live (200)"; i=$((i+1)); } || fail "expected 200 making court $cid live, got $code"
  else
    resp=$(owner -X PUT "$API/owner/venues/$VENUE_ID/courts/$cid/live" -d '{"live":true}')
    code=$(ostat -X PUT "$API/owner/venues/$VENUE_ID/courts/$cid/live" -d '{"live":true}')
    ecode=$(echo "$resp" | jq -r '.details.code // .error')
    { [ "$code" = "409" ] && [ "$ecode" = "COURT_LIVE_LIMIT" ]; } \
      && pass "over-limit court $cid rejected with single 409 COURT_LIVE_LIMIT" \
      || fail "expected 409 COURT_LIVE_LIMIT, got $code / $ecode"
    break
  fi
done

echo "== 4. Player sees only LIVE courts =="
PCOURTS=$(player "$API/venues/$VENUE_ID/courts" | jq '[.[]|.id]|sort')
LIVEIDS=$(owner "$API/venues/$VENUE_ID/courts" | jq '[.[]|select(.isLive==true)|.id]|sort')
[ "$PCOURTS" = "$LIVEIDS" ] && pass "player court-list == live set" || fail "player sees non-live courts! player=$PCOURTS live=$LIVEIDS"
VDETAIL=$(player "$API/venues/$VENUE_ID" | jq '[.courts[].id]|sort')
[ "$VDETAIL" = "$LIVEIDS" ] && pass "venue-details courts == live set" || fail "venue-details leaks non-live courts"

echo "== 5. Player cannot enumerate/book a LOCKED court =="
LOCKED_ONE=$(owner "$API/venues/$VENUE_ID/courts" | jq -r 'first(.[]|select(.isLive==false)|.id) // empty')
if [ -n "$LOCKED_ONE" ]; then
  scode=$(pstat "$API/courts/$LOCKED_ONE/slots?date=2030-01-01")
  [ "$scode" = "404" ] && pass "locked court slots -> 404 for player" || fail "locked court slots returned $scode (expected 404)"
  bcode=$(pstat -X POST "$API/bookings" \
    -d "{\"venueId\":$VENUE_ID,\"courtId\":$LOCKED_ONE,\"date\":\"2030-01-01\",\"startTime\":\"10:00\",\"endTime\":\"11:00\"}")
  [ "$bcode" != "200" ] && [ "$bcode" != "201" ] && pass "booking locked court rejected (HTTP $bcode)" || fail "booking a locked court was accepted!"
else
  echo "  (skip: no locked court available to probe)"
fi

echo "== 6. Disabling is always allowed + idempotent =="
LIVE_ONE=$(owner "$API/venues/$VENUE_ID/courts" | jq -r 'first(.[]|select(.isLive==true)|.id) // empty')
if [ -n "$LIVE_ONE" ]; then
  c1=$(ostat -X PUT "$API/owner/venues/$VENUE_ID/courts/$LIVE_ONE/live" -d '{"live":false}')
  c2=$(ostat -X PUT "$API/owner/venues/$VENUE_ID/courts/$LIVE_ONE/live" -d '{"live":false}')
  { [ "$c1" = "200" ] && [ "$c2" = "200" ]; } && pass "lock + re-lock both 200 (idempotent)" || fail "disable not idempotent ($c1,$c2)"
fi

echo
echo "ALL CHECKS PASSED."
echo "Manual (Part 4): downgrade the venue's plan to a lower court limit via the owner"
echo "subscription-request flow (owner picks which courts stay live, capped at the new limit) or via"
echo "admin edit; verify courts are never deleted, existing bookings remain, and the owner is notified"
echo "which courts were locked."
