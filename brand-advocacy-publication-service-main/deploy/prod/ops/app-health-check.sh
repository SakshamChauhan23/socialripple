#!/bin/bash
set -euo pipefail

MODE="${1:-health}"
ALERT_SCRIPT="${ALERT_SCRIPT:-/usr/local/sbin/socialripple-send-email}"
APP_FRONTEND_DOMAIN="${APP_FRONTEND_DOMAIN:-dashboard.socialripple.ai}"
APP_API_DOMAIN="${APP_API_DOMAIN:-api.socialripple.ai}"
DISK_THRESHOLD="${DISK_THRESHOLD:-85}"
MEMORY_THRESHOLD="${MEMORY_THRESHOLD:-90}"

failures=()
details=()
services=(
  "advocacy-user-management-prod.service"
  "advocacy-publication-service-prod.service"
  "advocacy-notification-service-prod.service"
  "advocacy-media-service-prod.service"
)
ports=(8080 8081 8082 8084)

record_failure() {
  failures+=("$1")
}

details+=("Host: $(hostname -f 2>/dev/null || hostname)")

for unit in "${services[@]}"; do
  status="$(systemctl is-active "${unit}" 2>/dev/null || true)"
  details+=("${unit}: ${status}")
  [ "${status}" = "active" ] || record_failure "${unit} is ${status}"
done

for port in "${ports[@]}"; do
  if ss -ltn "( sport = :${port} )" | grep -q ":${port}"; then
    details+=("port ${port}: listening")
  else
    details+=("port ${port}: down")
    record_failure "port ${port} is not listening"
  fi
done

frontend_status="$(curl -sk --resolve "${APP_FRONTEND_DOMAIN}:443:127.0.0.1" -o /dev/null -w '%{http_code}' "https://${APP_FRONTEND_DOMAIN}/" || true)"
api_status="$(curl -sk --resolve "${APP_API_DOMAIN}:443:127.0.0.1" -o /dev/null -w '%{http_code}' "https://${APP_API_DOMAIN}/v1/content/trending-topics" || true)"
publication_status="$(curl -sk --resolve "${APP_API_DOMAIN}:443:127.0.0.1" -o /dev/null -w '%{http_code}' "https://${APP_API_DOMAIN}/external-ingestion/" || true)"
media_status="$(curl -sk --resolve "${APP_API_DOMAIN}:443:127.0.0.1" -o /dev/null -w '%{http_code}' "https://${APP_API_DOMAIN}/api/media/content?file_id=test" || true)"

details+=("frontend smoke: ${frontend_status}")
details+=("user-management smoke: ${api_status}")
details+=("publication smoke: ${publication_status}")
details+=("media smoke: ${media_status}")

[ "${frontend_status}" = "200" ] || record_failure "frontend smoke returned ${frontend_status}"
[ "${api_status}" = "401" ] || record_failure "user-management smoke returned ${api_status}"
[ "${publication_status}" = "401" ] || record_failure "publication smoke returned ${publication_status}"
[ "${media_status}" = "404" ] || record_failure "media smoke returned ${media_status}"

disk_usage="$(df -P / | awk 'NR==2 {gsub(/%/, "", $5); print $5}')"
memory_usage="$(free | awk '/Mem:/ {printf "%.0f", ($3/$2)*100}')"

details+=("disk usage: ${disk_usage}%")
details+=("memory usage: ${memory_usage}%")

[ "${disk_usage}" -lt "${DISK_THRESHOLD}" ] || record_failure "disk usage ${disk_usage}% exceeds ${DISK_THRESHOLD}%"
[ "${memory_usage}" -lt "${MEMORY_THRESHOLD}" ] || record_failure "memory usage ${memory_usage}% exceeds ${MEMORY_THRESHOLD}%"

subject="[SocialRipple Prod] App VM ${MODE} summary"
body="$(printf '%s\n' "${details[@]}")"

if [ "${MODE}" = "summary" ]; then
  "${ALERT_SCRIPT}" "${subject}" "${body}" || true
  exit 0
fi

if [ "${#failures[@]}" -gt 0 ]; then
  subject="[SocialRipple Prod] App VM health check failed"
  body="$(printf 'Failures:\n%s\n\nDetails:\n%s\n' "$(printf '%s\n' "${failures[@]}")" "$(printf '%s\n' "${details[@]}")")"
  "${ALERT_SCRIPT}" "${subject}" "${body}" || true
  printf '%s\n' "${body}" >&2
  exit 1
fi

exit 0
