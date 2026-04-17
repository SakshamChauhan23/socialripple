#!/bin/bash
set -euo pipefail

MODE="${1:-health}"
ALERT_SCRIPT="${ALERT_SCRIPT:-/usr/local/sbin/socialripple-send-email}"
DB_PORT="${DB_PORT:-3306}"
DISK_THRESHOLD="${DISK_THRESHOLD:-80}"

failures=()
details=()

record_failure() {
  failures+=("$1")
}

details+=("Host: $(hostname -f 2>/dev/null || hostname)")

mysql_status="$(systemctl is-active mysql 2>/dev/null || true)"
details+=("mysql.service: ${mysql_status}")
[ "${mysql_status}" = "active" ] || record_failure "mysql.service is ${mysql_status}"

if ss -ltn "( sport = :${DB_PORT} )" | grep -q ":${DB_PORT}"; then
  details+=("port ${DB_PORT}: listening")
else
  details+=("port ${DB_PORT}: down")
  record_failure "port ${DB_PORT} is not listening"
fi

if mysql -Nse "SELECT 1" >/dev/null 2>&1; then
  details+=("mysql probe: ok")
else
  details+=("mysql probe: failed")
  record_failure "mysql probe failed"
fi

disk_usage="$(df -P / | awk 'NR==2 {gsub(/%/, "", $5); print $5}')"
details+=("disk usage: ${disk_usage}%")
[ "${disk_usage}" -lt "${DISK_THRESHOLD}" ] || record_failure "disk usage ${disk_usage}% exceeds ${DISK_THRESHOLD}%"

subject="[SocialRipple Prod] DB VM ${MODE} summary"
body="$(printf '%s\n' "${details[@]}")"

if [ "${MODE}" = "summary" ]; then
  "${ALERT_SCRIPT}" "${subject}" "${body}" || true
  exit 0
fi

if [ "${#failures[@]}" -gt 0 ]; then
  subject="[SocialRipple Prod] DB VM health check failed"
  body="$(printf 'Failures:\n%s\n\nDetails:\n%s\n' "$(printf '%s\n' "${failures[@]}")" "$(printf '%s\n' "${details[@]}")")"
  "${ALERT_SCRIPT}" "${subject}" "${body}" || true
  printf '%s\n' "${body}" >&2
  exit 1
fi

exit 0
