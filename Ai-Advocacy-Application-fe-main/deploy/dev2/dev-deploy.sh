#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

REACT_APP_RECAPTCHA_SITE_KEY="${REACT_APP_RECAPTCHA_SITE_KEY:-6Le2KJcsAAAAALM7BySfVQC8lqFKobEFA_53jr2k}"
DEV_USER_API_DOMAIN="${DEV_USER_API_DOMAIN:-${DEV_API_DOMAIN:-}}"
DEV_PLATFORM_API_DOMAIN="${DEV_PLATFORM_API_DOMAIN:-${DEV_API_DOMAIN:-${DEV_USER_API_DOMAIN:-}}}"
DEPLOY_COMMIT_SHA="${DEPLOY_COMMIT_SHA:-}"
DEPLOY_BRANCH_NAME="${DEPLOY_BRANCH_NAME:-}"

SOURCE_DIR="/github/dev2/advocacy-fe"
WEBROOT="/var/www/advocacy-dev.moonhive-server.in.net/html"

if [ -z "${DEV_USER_API_DOMAIN}" ]; then
  echo "DEV_USER_API_DOMAIN or DEV_API_DOMAIN is required"
  exit 1
fi

if [ -z "${DEV_PLATFORM_API_DOMAIN}" ]; then
  echo "DEV_PLATFORM_API_DOMAIN or DEV_API_DOMAIN is required"
  exit 1
fi

cd "${SOURCE_DIR}"

rm -rf build node_modules package-lock.json

cat > .env.production.local <<ENVEOF
REACT_APP_USER_API_ROOT=https://${DEV_USER_API_DOMAIN}
REACT_APP_USER_API_URL=https://${DEV_USER_API_DOMAIN}/v1/
REACT_APP_PLATFORM_API_ROOT=https://${DEV_PLATFORM_API_DOMAIN}
REACT_APP_PLATFORM_API_URL=https://${DEV_PLATFORM_API_DOMAIN}/
REACT_APP_API_URL=https://${DEV_USER_API_DOMAIN}/v1/
REACT_APP_API_URL_EXTERNAL=https://${DEV_PLATFORM_API_DOMAIN}/
REACT_APP_USER_WS_URL=wss://${DEV_USER_API_DOMAIN}
REACT_APP_WS_URL=wss://${DEV_USER_API_DOMAIN}
REACT_APP_WS_PATH=/ws
REACT_APP_RECAPTCHA_SITE_KEY=${REACT_APP_RECAPTCHA_SITE_KEY}
ENVEOF

npm install
npm run build

build_commit_sha="${DEPLOY_COMMIT_SHA:-unknown}"
build_branch="${DEPLOY_BRANCH_NAME:-unknown}"
build_timestamp_utc="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

cat > build/build-info.json <<INFOEOF
{
  "commitSha": "${build_commit_sha}",
  "branch": "${build_branch}",
  "builtAtUtc": "${build_timestamp_utc}"
}
INFOEOF

mkdir -p "${WEBROOT}"
rm -rf "${WEBROOT}"/*
cp -R build/. "${WEBROOT}/"

echo "Dev2 frontend deployed to ${WEBROOT}"
