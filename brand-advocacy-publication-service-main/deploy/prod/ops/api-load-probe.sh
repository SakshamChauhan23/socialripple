#!/bin/bash
set -euo pipefail

TARGET_URL="${TARGET_URL:-https://api.socialripple.ai/v1/content/trending-topics}"
REQUESTS="${REQUESTS:-50}"
CONCURRENCY="${CONCURRENCY:-10}"
HTTP_METHOD="${HTTP_METHOD:-GET}"
REQUEST_BODY="${REQUEST_BODY:-}"
CONTENT_TYPE="${CONTENT_TYPE:-application/json}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
EXTRA_HEADERS="${EXTRA_HEADERS:-}"
OUTPUT_DIR="$(mktemp -d)"
RESULTS_FILE="${OUTPUT_DIR}/results.txt"

cleanup() {
  rm -rf "${OUTPUT_DIR}"
}
trap cleanup EXIT

run_probe_once() {
  local start end code
  local -a curl_args

  curl_args=(-k -s -o /dev/null -w "%{http_code}" -X "${HTTP_METHOD}")

  if [ -n "${CONTENT_TYPE}" ]; then
    curl_args+=(-H "Content-Type: ${CONTENT_TYPE}")
  fi

  if [ -n "${AUTH_TOKEN}" ]; then
    curl_args+=(-H "Authorization: Bearer ${AUTH_TOKEN}")
  fi

  if [ -n "${EXTRA_HEADERS}" ]; then
    while IFS= read -r header; do
      [ -n "${header}" ] || continue
      curl_args+=(-H "${header}")
    done <<< "${EXTRA_HEADERS}"
  fi

  if [ -n "${REQUEST_BODY}" ]; then
    curl_args+=(--data "${REQUEST_BODY}")
  fi

  curl_args+=("${TARGET_URL}")

  start=$(date +%s%3N)
  code="$(curl "${curl_args[@]}")"
  end=$(date +%s%3N)

  printf "%s %s\n" "${code}" "$((end-start))" >> "${RESULTS_FILE}"
}

for _ in $(seq "${REQUESTS}"); do
  run_probe_once &
  while [ "$(jobs -rp | wc -l)" -ge "${CONCURRENCY}" ]; do
    wait -n
  done
done

wait

echo "target=${TARGET_URL}"
echo "requests=${REQUESTS}"
echo "concurrency=${CONCURRENCY}"
echo "method=${HTTP_METHOD}"
echo "auth=$( [ -n "${AUTH_TOKEN}" ] && echo enabled || echo disabled )"
echo

echo "[status_counts]"
awk '{count[$1]++} END {for (code in count) print code, count[code]}' "${RESULTS_FILE}" | sort
echo

echo "[latency_ms]"
awk '
  {vals[NR]=$2; sum+=$2}
  END {
    if (NR == 0) exit 0
    asort(vals)
    p50=vals[int((NR+1)*0.50)]
    p95=vals[int((NR+1)*0.95)]
    p99=vals[int((NR+1)*0.99)]
    printf "avg=%.2f\np50=%s\np95=%s\np99=%s\nmax=%s\n", sum/NR, p50, p95, p99, vals[NR]
  }
' "${RESULTS_FILE}"
