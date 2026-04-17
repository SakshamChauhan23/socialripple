#!/bin/bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-https://api.socialripple.ai}"
REQUESTS="${REQUESTS:-100}"
CONCURRENCY="${CONCURRENCY:-20}"
OUTPUT_ROOT="${OUTPUT_ROOT:-/var/log/socialripple-performance}"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
RUN_DIR="${OUTPUT_ROOT}/${RUN_ID}"
SCENARIO="${SCENARIO:-unauthenticated_control}"
PERF_ENV_FILE="${PERF_ENV_FILE:-/etc/advocacy/perf-baseline.env}"
PERF_BASELINE_AUTH_MODE="${PERF_BASELINE_AUTH_MODE:-existing_jwt}"
CONTENT_GENERATION_TOPIC="${CONTENT_GENERATION_TOPIC:-Employee advocacy trends for B2B SaaS in 2026}"

mkdir -p "${RUN_DIR}"

if [ -f "${PERF_ENV_FILE}" ]; then
  # shellcheck disable=SC1090
  source "${PERF_ENV_FILE}"
fi

generate_header_values() {
  local trace_id correlation_id
  trace_id="trace-baseline-$(date +%s%N)"
  correlation_id="corr-baseline-$(date +%s%N)"
  cat <<EOF
x-trace-id: ${trace_id}
x-correlation-id: ${correlation_id}
Language_id: en
language-id: en
EOF
}

require_authenticated_env() {
  local missing=()

  case "${PERF_BASELINE_AUTH_MODE}" in
    existing_jwt)
      [ -n "${PERF_BASELINE_AUTH_TOKEN:-}" ] || missing+=("PERF_BASELINE_AUTH_TOKEN")
      [ -n "${PERF_BASELINE_TENANT_ID:-}" ] || missing+=("PERF_BASELINE_TENANT_ID")
      ;;
    login_flow)
      [ -n "${PERF_BASELINE_TEST_USERNAME:-}" ] || missing+=("PERF_BASELINE_TEST_USERNAME")
      [ -n "${PERF_BASELINE_TEST_PASSWORD:-}" ] || missing+=("PERF_BASELINE_TEST_PASSWORD")
      [ -n "${PERF_BASELINE_TEST_CAPTCHA_TOKEN:-}" ] || missing+=("PERF_BASELINE_TEST_CAPTCHA_TOKEN")
      [ -n "${PERF_BASELINE_TENANT_ID:-}" ] || missing+=("PERF_BASELINE_TENANT_ID")
      ;;
    *)
      echo "Unsupported PERF_BASELINE_AUTH_MODE: ${PERF_BASELINE_AUTH_MODE}" >&2
      exit 1
      ;;
  esac

  if [ "${#missing[@]}" -gt 0 ]; then
    echo "Missing authenticated baseline config: ${missing[*]}" >&2
    exit 1
  fi
}

login_and_get_token() {
  local response body status token
  local login_headers
  local -a curl_args

  login_headers="$(generate_header_values)"
  curl_args=(
    -sk
    -H "Content-Type: application/json"
  )

  while IFS= read -r line; do
    [ -n "${line}" ] || continue
    curl_args+=(-H "${line}")
  done <<< "${login_headers}"

  curl_args+=(
    --data "{\"userName\":\"${PERF_BASELINE_TEST_USERNAME}\",\"password\":\"${PERF_BASELINE_TEST_PASSWORD}\",\"captchaToken\":\"${PERF_BASELINE_TEST_CAPTCHA_TOKEN}\"}"
    -w $'\n%{http_code}'
    "${API_BASE_URL}/v1/auth/login"
  )

  response="$(curl "${curl_args[@]}")"

  body="${response%$'\n'*}"
  status="${response##*$'\n'}"
  printf '%s\n' "${body}" > "${RUN_DIR}/auth-login-response.json"
  printf 'status=%s\n' "${status}" > "${RUN_DIR}/auth-login-meta.env"

  if [ "${status}" != "200" ]; then
    echo "Authenticated baseline login failed with HTTP ${status}" >&2
    exit 1
  fi

  token="$(printf '%s' "${body}" | python3 -c 'import json,sys; print((json.load(sys.stdin).get("token") or "").strip())')"

  if [ -z "${token}" ]; then
    echo "Authenticated baseline login returned no token" >&2
    exit 1
  fi

  printf '%s' "${token}"
}

resolve_authenticated_token() {
  case "${PERF_BASELINE_AUTH_MODE}" in
    existing_jwt)
      printf '%s' "${PERF_BASELINE_AUTH_TOKEN}"
      ;;
    login_flow)
      login_and_get_token
      ;;
  esac
}

run_probe() {
  local output_name="$1"
  local target_url="$2"
  local method="${3:-GET}"
  local request_body="${4:-}"
  local auth_token="${5:-}"
  local extra_headers="${6:-}"

  TARGET_URL="${target_url}" \
  HTTP_METHOD="${method}" \
  REQUEST_BODY="${request_body}" \
  AUTH_TOKEN="${auth_token}" \
  EXTRA_HEADERS="${extra_headers}" \
  REQUESTS="${REQUESTS}" \
  CONCURRENCY="${CONCURRENCY}" \
  /usr/local/sbin/socialripple-api-load-probe > "${RUN_DIR}/${output_name}.txt"
}

run_authenticated_read_probes() {
  local auth_token="$1"
  local auth_headers="$2"

  run_probe "auth-user-profile" "${API_BASE_URL}/v1/user/profile" "GET" "" "${auth_token}" "${auth_headers}"
  run_probe "auth-dashboard-engagement" "${API_BASE_URL}/v1/api/dashboard/employee/engagement-metrics" "GET" "" "${auth_token}" "${auth_headers}"
  run_probe "auth-dashboard-top-contributors" "${API_BASE_URL}/v1/api/dashboard/employee/top-contributors" "GET" "" "${auth_token}" "${auth_headers}"
}

run_content_generation_probes() {
  local auth_token="$1"
  local auth_headers="$2"
  local request_body

  request_body="$(printf '{"topic":"%s"}' "${CONTENT_GENERATION_TOPIC}")"
  run_probe "auth-content-generate" "${API_BASE_URL}/v1/content/generate" "POST" "${request_body}" "${auth_token}" "${auth_headers}"
}

run_publication_fetch_probes() {
  local auth_token="$1"
  local auth_headers="$2"

  run_probe "auth-publication-fetch-x" "${API_BASE_URL}/external-ingestion/v1/api/x/fetch/x" "GET" "" "${auth_token}" "${auth_headers}"
  run_probe "auth-publication-fetch-linkedin" "${API_BASE_URL}/external-ingestion/v1/api/x/fetch/linkedin" "GET" "" "${auth_token}" "${auth_headers}"
  run_probe "auth-publication-fetch-linkedin-v" "${API_BASE_URL}/external-ingestion/v1/api/x/fetch/linkedin-v" "GET" "" "${auth_token}" "${auth_headers}"
}

{
  echo "run_id=${RUN_ID}"
  echo "timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "api_base_url=${API_BASE_URL}"
  echo "requests=${REQUESTS}"
  echo "concurrency=${CONCURRENCY}"
  echo "scenario=${SCENARIO}"
  echo "auth_mode=${PERF_BASELINE_AUTH_MODE}"
} > "${RUN_DIR}/meta.env"

/usr/local/sbin/socialripple-app-performance-snapshot > "${RUN_DIR}/app-before.txt"

run_probe "trending" "${API_BASE_URL}/v1/content/trending-topics"
run_probe "external-ingestion" "${API_BASE_URL}/external-ingestion/"
run_probe "media" "${API_BASE_URL}/api/media/content?file_id=test"
run_probe "login" "${API_BASE_URL}/v1/auth/login" "POST" '{"userName":"invalid@example.com","password":"bad","captchaToken":"bad"}'

if [ "${SCENARIO}" != "unauthenticated_control" ]; then
  require_authenticated_env
  auth_token="$(resolve_authenticated_token)"
  auth_headers="$(generate_header_values)"$'\n'"x-tenant-id: ${PERF_BASELINE_TENANT_ID}"

  {
    echo "auth_mode=${PERF_BASELINE_AUTH_MODE}"
    echo "username=${PERF_BASELINE_TEST_USERNAME:-}"
    echo "tenant_id=${PERF_BASELINE_TENANT_ID}"
    echo "expires_at=${PERF_BASELINE_AUTH_EXPIRES_AT:-}"
  } > "${RUN_DIR}/auth-session-meta.env"

  case "${SCENARIO}" in
    authenticated_core|auth_read_heavy)
      run_authenticated_read_probes "${auth_token}" "${auth_headers}"
      ;;
    content_generation_heavy)
      run_content_generation_probes "${auth_token}" "${auth_headers}"
      ;;
    publication_heavy)
      run_publication_fetch_probes "${auth_token}" "${auth_headers}"
      ;;
    mixed_realistic)
      run_authenticated_read_probes "${auth_token}" "${auth_headers}"
      run_content_generation_probes "${auth_token}" "${auth_headers}"
      run_publication_fetch_probes "${auth_token}" "${auth_headers}"
      ;;
    *)
      echo "Unsupported scenario: ${SCENARIO}" >&2
      exit 1
      ;;
  esac
fi

/usr/local/sbin/socialripple-app-performance-snapshot > "${RUN_DIR}/app-after.txt"

echo "performance baseline written to ${RUN_DIR}"
