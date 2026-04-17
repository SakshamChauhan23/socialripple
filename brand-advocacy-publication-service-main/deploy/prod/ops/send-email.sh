#!/bin/bash
set -euo pipefail

SUBJECT="${1:?subject is required}"
BODY="${2:-}"
ALERT_CONFIG="${ALERT_CONFIG:-/etc/advocacy/ops-alert.env}"

if [ -z "${BODY}" ] && [ ! -t 0 ]; then
  BODY="$(cat)"
fi

if [ -f "${ALERT_CONFIG}" ]; then
  # shellcheck disable=SC1090
  . "${ALERT_CONFIG}"
fi

PROVIDER="${ALERT_EMAIL_PROVIDER:-log}"
TO="${ALERT_EMAIL_TO:-devops@moonhive.in}"
FROM="${ALERT_EMAIL_FROM:-socialripple-alerts@localhost}"

json_escape() {
  python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))'
}

log_only() {
  logger -t socialripple-alerts "${SUBJECT}"
  printf '%s\n' "${BODY}" >&2
}

case "${PROVIDER}" in
  sendgrid)
    : "${ALERT_EMAIL_API_KEY:?ALERT_EMAIL_API_KEY is required for sendgrid}"
    SUBJECT_JSON="$(printf '%s' "${SUBJECT}" | json_escape)"
    BODY_JSON="$(printf '%s' "${BODY}" | json_escape)"
    FROM_JSON="$(printf '%s' "${FROM}" | json_escape)"
    TO_JSON="$(printf '%s' "${TO}" | json_escape)"
    curl -fsS https://api.sendgrid.com/v3/mail/send \
      -H "Authorization: Bearer ${ALERT_EMAIL_API_KEY}" \
      -H "Content-Type: application/json" \
      -d "{\"personalizations\":[{\"to\":[{\"email\":${TO_JSON}}]}],\"from\":{\"email\":${FROM_JSON}},\"subject\":${SUBJECT_JSON},\"content\":[{\"type\":\"text/plain\",\"value\":${BODY_JSON}}]}"
    ;;
  resend)
    : "${ALERT_EMAIL_API_KEY:?ALERT_EMAIL_API_KEY is required for resend}"
    SUBJECT_JSON="$(printf '%s' "${SUBJECT}" | json_escape)"
    BODY_JSON="$(printf '%s' "${BODY}" | json_escape)"
    FROM_JSON="$(printf '%s' "${FROM}" | json_escape)"
    TO_JSON="$(printf '%s' "${TO}" | json_escape)"
    curl -fsS https://api.resend.com/emails \
      -H "Authorization: Bearer ${ALERT_EMAIL_API_KEY}" \
      -H "Content-Type: application/json" \
      -d "{\"from\":${FROM_JSON},\"to\":[${TO_JSON}],\"subject\":${SUBJECT_JSON},\"text\":${BODY_JSON}}"
    ;;
  mailgun)
    : "${ALERT_EMAIL_API_KEY:?ALERT_EMAIL_API_KEY is required for mailgun}"
    : "${ALERT_EMAIL_MAILGUN_DOMAIN:?ALERT_EMAIL_MAILGUN_DOMAIN is required for mailgun}"
    curl -fsS --user "api:${ALERT_EMAIL_API_KEY}" \
      "https://api.mailgun.net/v3/${ALERT_EMAIL_MAILGUN_DOMAIN}/messages" \
      -F "from=${FROM}" \
      -F "to=${TO}" \
      -F "subject=${SUBJECT}" \
      -F "text=${BODY}"
    ;;
  log|"")
    log_only
    ;;
  *)
    echo "Unsupported alert provider: ${PROVIDER}" >&2
    log_only
    ;;
esac
