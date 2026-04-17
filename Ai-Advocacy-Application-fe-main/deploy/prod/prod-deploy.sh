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
: "${PROD_API_DOMAIN:?PROD_API_DOMAIN is required}"
: "${REACT_APP_RECAPTCHA_SITE_KEY:?REACT_APP_RECAPTCHA_SITE_KEY is required}"

SOURCE_DIR="/github/prod/advocacy-fe"
git config --global --add safe.directory "${SOURCE_DIR}" || true
RELEASE_SHA="$(git -C "${SOURCE_DIR}" rev-parse HEAD)"
WEBROOT_BASE="${PROD_FE_WEBROOT:-/var/www/${PROD_FRONTEND_DOMAIN}}"
RELEASES_DIR="${WEBROOT_BASE}/releases"
RELEASE_DIR="${WEBROOT_BASE}/releases/${RELEASE_SHA}"
CURRENT_LINK="${WEBROOT_BASE}/current"
BUILD_DIR="${SOURCE_DIR}/build"
RELEASE_RETENTION="${RELEASE_RETENTION:-5}"

smoke_check() {
  local attempt
  local status

  for attempt in 1 2 3 4 5; do
    status="$(curl -s -o /dev/null -w '%{http_code}' -H "Host: ${PROD_FRONTEND_DOMAIN}" http://127.0.0.1/ || true)"
    if [ "${status}" = "200" ] || [ "${status}" = "301" ]; then
      return 0
    fi
    sleep 2
  done

  echo "Frontend smoke check failed for ${PROD_FRONTEND_DOMAIN}"
  return 1
}

cd "${SOURCE_DIR}"

rm -rf build node_modules

cat > .env.production.local <<EOF
REACT_APP_API_URL=https://${PROD_API_DOMAIN}/v1/
REACT_APP_API_URL_EXTERNAL=https://${PROD_API_DOMAIN}/
REACT_APP_WS_URL=wss://${PROD_API_DOMAIN}
REACT_APP_WS_PATH=/ws
REACT_APP_RECAPTCHA_SITE_KEY=${REACT_APP_RECAPTCHA_SITE_KEY}
REACT_APP_GOOGLE_SSO_CLIENT_ID=${REACT_APP_GOOGLE_SSO_CLIENT_ID:-}
REACT_APP_MICROSOFT_CLIENT_ID=${REACT_APP_MICROSOFT_CLIENT_ID:-}
REACT_APP_MICROSOFT_TENANT_ID=${REACT_APP_MICROSOFT_TENANT_ID:-}
EOF

npm install
npm run build

if [ ! -f "${BUILD_DIR}/index.html" ]; then
  echo "Production frontend build output not found"
  exit 1
fi

mkdir -p "${RELEASES_DIR}" "${RELEASE_DIR}"
rm -rf "${RELEASE_DIR:?}"/*
cp -R "${BUILD_DIR}/." "${RELEASE_DIR}/"
ln -sfn "${RELEASE_DIR}" "${CURRENT_LINK}"

smoke_check

if [ "${RELEASE_RETENTION}" -gt 0 ]; then
  ls -1dt "${RELEASES_DIR}"/* 2>/dev/null | tail -n +"$((RELEASE_RETENTION + 1))" | xargs -r rm -rf
fi

echo "Production frontend deployed to ${CURRENT_LINK}"
