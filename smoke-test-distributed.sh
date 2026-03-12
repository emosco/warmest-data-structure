#!/usr/bin/env bash

set -euo pipefail

BASE_8080="${BASE_8080:-http://localhost:8080}"
BASE_8081="${BASE_8081:-http://localhost:8081}"
BASE_8082="${BASE_8082:-http://localhost:8082}"

TEST_ID="$(date +%s)"
KEY_A="smoke-a-${TEST_ID}"
KEY_B="smoke-b-${TEST_ID}"

TEMP_BODY="$(mktemp)"
trap 'rm -f "$TEMP_BODY"' EXIT

wait_for_server() {
  local base_url="$1"
  local attempts=30

  for ((i=1; i<=attempts; i++)); do
    if curl -fsS "${base_url}/v3/api-docs" > /dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  echo "Server did not become ready: ${base_url}" >&2
  exit 1
}

request() {
  local method="$1"
  local url="$2"
  local data="${3:-}"

  local http_code

  if [[ -n "$data" ]]; then
    http_code="$(
      curl -sS -o "$TEMP_BODY" -w "%{http_code}" \
        -X "$method" \
        -H "Content-Type: application/json" \
        "$url" \
        -d "$data"
    )"
  else
    http_code="$(
      curl -sS -o "$TEMP_BODY" -w "%{http_code}" \
        -X "$method" \
        "$url"
    )"
  fi

  RESPONSE_CODE="$http_code"
  RESPONSE_BODY="$(cat "$TEMP_BODY")"
}

assert_status() {
  local expected="$1"
  local label="$2"

  if [[ "$RESPONSE_CODE" != "$expected" ]]; then
    echo "FAIL: ${label}" >&2
    echo "Expected status: ${expected}" >&2
    echo "Actual status:   ${RESPONSE_CODE}" >&2
    echo "Body: ${RESPONSE_BODY}" >&2
    exit 1
  fi
}

assert_body() {
  local expected="$1"
  local label="$2"

  if [[ "$RESPONSE_BODY" != "$expected" ]]; then
    echo "FAIL: ${label}" >&2
    echo "Expected body: ${expected}" >&2
    echo "Actual body:   ${RESPONSE_BODY}" >&2
    exit 1
  fi
}

echo "Waiting for distributed instances..."
wait_for_server "$BASE_8080"
wait_for_server "$BASE_8081"
wait_for_server "$BASE_8082"

echo "Using test keys: ${KEY_A}, ${KEY_B}"

request PUT "${BASE_8080}/api/v1/warmest/${KEY_A}" '{"value":100}'
assert_status "200" "PUT ${KEY_A} on 8080"

request GET "${BASE_8081}/api/v1/warmest/${KEY_A}"
assert_status "200" "GET ${KEY_A} on 8081"
assert_body "100" "GET ${KEY_A} should return 100 on 8081"

request GET "${BASE_8082}/api/v1/warmest"
assert_status "200" "GET warmest on 8082 after reading ${KEY_A}"
assert_body "${KEY_A}" "Warmest should be ${KEY_A} on 8082"

request PUT "${BASE_8081}/api/v1/warmest/${KEY_B}" '{"value":200}'
assert_status "200" "PUT ${KEY_B} on 8081"

request GET "${BASE_8080}/api/v1/warmest"
assert_status "200" "GET warmest on 8080 after writing ${KEY_B}"
assert_body "${KEY_B}" "Warmest should be ${KEY_B} on 8080"

request GET "${BASE_8082}/api/v1/warmest/${KEY_A}"
assert_status "200" "GET ${KEY_A} on 8082"
assert_body "100" "GET ${KEY_A} should still return 100 on 8082"

request GET "${BASE_8081}/api/v1/warmest"
assert_status "200" "GET warmest on 8081 after re-reading ${KEY_A}"
assert_body "${KEY_A}" "Warmest should be ${KEY_A} on 8081"

request DELETE "${BASE_8080}/api/v1/warmest/${KEY_A}"
assert_status "200" "DELETE ${KEY_A} on 8080"
assert_body "100" "DELETE ${KEY_A} should return 100 on 8080"

request GET "${BASE_8082}/api/v1/warmest"
assert_status "200" "GET warmest on 8082 after deleting ${KEY_A}"
assert_body "${KEY_B}" "Warmest should fall back to ${KEY_B} on 8082"

echo "PASS: distributed smoke test succeeded across ports 8080, 8081, and 8082."
