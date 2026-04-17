#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

APP_DIR="/opt/advocacy/publication-service-prod"
UNIT_NAME="advocacy-publication-service-prod.service"
PROD_API_DOMAIN="${PROD_API_DOMAIN:-api.socialripple.ai}"
TARGET_RELEASE="${1:-}"
MAX_SMOKE_ATTEMPTS="${MAX_SMOKE_ATTEMPTS:-15}"
SMOKE_SLEEP_SECONDS="${SMOKE_SLEEP_SECONDS:-3}"

smoke_check() {
  local attempt
  local status

  for attempt in $(seq 1 "${MAX_SMOKE_ATTEMPTS}"); do
    status="$(curl -sk --resolve "${PROD_API_DOMAIN}:443:127.0.0.1" -o /dev/null -w '%{http_code}' "https://${PROD_API_DOMAIN}/external-ingestion/" || true)"
    if [ "${status}" = "401" ]; then
      return 0
    fi
    sleep "${SMOKE_SLEEP_SECONDS}"
  done

  echo "Publication smoke check failed"
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
