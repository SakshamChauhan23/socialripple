#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

APP_DIR="/opt/advocacy/notification-service-prod"
UNIT_NAME="advocacy-notification-service-prod.service"
SERVER_PORT="${SERVER_PORT:-8082}"
TARGET_RELEASE="${1:-}"
MAX_SMOKE_ATTEMPTS="${MAX_SMOKE_ATTEMPTS:-15}"
SMOKE_SLEEP_SECONDS="${SMOKE_SLEEP_SECONDS:-3}"

smoke_check() {
  local attempt
  local status

  for attempt in $(seq 1 "${MAX_SMOKE_ATTEMPTS}"); do
    status="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${SERVER_PORT}/" || true)"
    case "${status}" in
      200|401|403|404)
        return 0
        ;;
    esac
    sleep "${SMOKE_SLEEP_SECONDS}"
  done

  echo "Notification smoke check failed"
  return 1
}

if [ -n "${TARGET_RELEASE}" ]; then
  ROLLBACK_DIR="${APP_DIR}/releases/${TARGET_RELEASE}"
else
  ROLLBACK_DIR="$(ls -1dt "${APP_DIR}/releases"/* 2>/dev/null | sed -n '2p')"
fi

if [ -z "${ROLLBACK_DIR:-}" ] || [ ! -d "${ROLLBACK_DIR}" ]; then
  echo "Rollback release not found"
  exit 1
fi

ln -sfn "${ROLLBACK_DIR}/app.jar" "${APP_DIR}/current/app.jar"
systemctl restart "${UNIT_NAME}"
systemctl is-active --quiet "${UNIT_NAME}"
smoke_check

echo "Rolled back ${UNIT_NAME} to ${ROLLBACK_DIR}"
