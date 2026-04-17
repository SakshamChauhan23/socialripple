#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

: "${PROD_FRONTEND_DOMAIN:?PROD_FRONTEND_DOMAIN is required}"

WEBROOT_BASE="${PROD_FE_WEBROOT:-/var/www/${PROD_FRONTEND_DOMAIN}}"
RELEASES_DIR="${WEBROOT_BASE}/releases"
CURRENT_LINK="${WEBROOT_BASE}/current"
TARGET_RELEASE="${1:-}"

smoke_check() {
  local attempt
  local status

  for attempt in 1 2 3 4 5; do
    status="$(curl -s -o /dev/null -w '%{http_code}' -H "Host: ${PROD_FRONTEND_DOMAIN}" http://127.0.0.1/ || true)"
    if [ "${status}" = "200" ]; then
      return 0
    fi
    sleep 2
  done

  echo "Frontend smoke check failed for ${PROD_FRONTEND_DOMAIN}"
  return 1
}

if [ -n "${TARGET_RELEASE}" ]; then
  ROLLBACK_DIR="${RELEASES_DIR}/${TARGET_RELEASE}"
else
  ROLLBACK_DIR="$(ls -1dt "${RELEASES_DIR}"/* 2>/dev/null | sed -n '2p')"
fi

if [ -z "${ROLLBACK_DIR:-}" ] || [ ! -d "${ROLLBACK_DIR}" ]; then
  echo "Rollback release not found"
  exit 1
fi

ln -sfn "${ROLLBACK_DIR}" "${CURRENT_LINK}"
smoke_check

echo "Frontend rolled back to ${ROLLBACK_DIR}"
