#!/usr/bin/env bash
# Smoke test for a running Courier Tracking API.
#
# Usage:   scripts/smoke-test.sh [base-url]
# Default: http://localhost:8080 (override with the argument or BASE_URL)
# Needs:   bash and curl. Uses a fresh courier id per run, so it is safe to re-run.
# Exit:    0 if every check passes, 1 otherwise.

set -u

BASE_URL="${1:-${BASE_URL:-http://localhost:8080}}"
BASE_URL="${BASE_URL%/}"
READY_TIMEOUT="${READY_TIMEOUT:-60}" # seconds to wait for the API

RUN_ID="$(date +%s)-$$"
COURIER="smoke-${RUN_ID}"
UNKNOWN_COURIER="smoke-unknown-${RUN_ID}"
STORE_ID="7149e322-5c08-3471-942d-311309121d39" # Ataşehir MMM Migros (name-derived UUID v3)
STORE_LAT="40.9923307"
STORE_LNG="29.1244229"

BODY_FILE="$(mktemp)"
trap 'rm -f "$BODY_FILE"' EXIT
STATUS=""
BODY=""
failures=0

call() { # call METHOD PATH [JSON_BODY] -> sets STATUS and BODY
  local method="$1" path="$2" data="${3:-}"
  local args=(-s -o "$BODY_FILE" -w '%{http_code}' -X "$method" "${BASE_URL}${path}")
  if [ -n "$data" ]; then
    args+=(-H 'Content-Type: application/json' -d "$data")
  fi
  : >"$BODY_FILE"
  STATUS="$(curl "${args[@]}")" || STATUS="000"
  BODY="$(cat "$BODY_FILE")"
}

check() { # check DESCRIPTION EXPECTED_STATUS [BODY_SUBSTRING...]
  local desc="$1" expected="$2" needle ok=1
  shift 2
  [ "$STATUS" = "$expected" ] || ok=0
  for needle in "$@"; do
    [[ "$BODY" == *"$needle"* ]] || ok=0
  done
  if [ "$ok" = 1 ]; then
    echo "PASS  $desc"
  else
    echo "FAIL  $desc (expected HTTP $expected${*:+ containing: $*}; got HTTP $STATUS: $BODY)"
    failures=$((failures + 1))
  fi
}

location_json() { # location_json TIMESTAMP LAT LNG
  printf '{"courierId":"%s","timestamp":"%s","latitude":%s,"longitude":%s}' "$COURIER" "$1" "$2" "$3"
}

echo "Waiting for ${BASE_URL} (up to ${READY_TIMEOUT}s)..."
ready=0
waited=0
while [ "$waited" -lt "$READY_TIMEOUT" ]; do
  code="$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}/api/v1/stores" 2>/dev/null || true)"
  if [ "$code" = "200" ]; then
    ready=1
    break
  fi
  sleep 2
  waited=$((waited + 2))
done
if [ "$ready" != 1 ]; then
  echo "API not reachable at ${BASE_URL}. Start it first (see README)." >&2
  exit 1
fi

call GET /api/v1/stores
check "GET stores lists the configured stores" 200 "$STORE_ID"

call POST /api/v1/locations "$(location_json 2024-01-01T00:01:00Z "$STORE_LAT" "$STORE_LNG")"
check "POST first point inside a store -> 201, applied, entrance event" 201 '"applied":true' "$STORE_ID"

call POST /api/v1/locations "$(location_json 2024-01-01T00:01:00Z "$STORE_LAT" "$STORE_LNG")"
check "POST identical point again -> 200 (idempotent)" 200

call POST /api/v1/locations "$(location_json 2024-01-01T00:01:00Z 41.0 29.0)"
check "POST same timestamp, different coordinates -> 409" 409

call POST /api/v1/locations "$(location_json 2024-01-01T00:00:00Z 41.0 29.0)"
check "POST earlier timestamp -> 201, not applied" 201 '"applied":false'

call POST /api/v1/locations "$(location_json 2024-01-01T00:02:00Z 100 29.0)"
check "POST latitude out of range -> 400" 400

call GET "/api/v1/couriers/${COURIER}/distance"
check "GET distance for known courier" 200 "\"courierId\":\"${COURIER}\"" '"totalDistanceMeters"'

call GET "/api/v1/couriers/${UNKNOWN_COURIER}/distance"
check "GET distance for unknown courier -> 404" 404

call GET "/api/v1/entrance-events?courierId=${COURIER}"
check "GET entrance events for the courier" 200 "$STORE_ID"

echo
if [ "$failures" -eq 0 ]; then
  echo "All checks passed."
  exit 0
fi
echo "${failures} check(s) failed."
exit 1
