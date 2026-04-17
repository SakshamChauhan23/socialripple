#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

SOURCE_DIR="/github/prod/user-management"
APP_DIR="/opt/advocacy/user-management-prod"
UNIT_NAME="advocacy-user-management-prod.service"
SERVER_PORT="${SERVER_PORT:-8081}"
SPRING_PROFILE="prod"
JAVA_HOME="/opt/jdk-21"
MAVEN_BIN="/opt/apache-maven/bin/mvn"
PROD_API_DOMAIN="${PROD_API_DOMAIN:-api.socialripple.ai}"
RELEASE_RETENTION="${RELEASE_RETENTION:-5}"
MAX_SMOKE_ATTEMPTS="${MAX_SMOKE_ATTEMPTS:-30}"
SMOKE_SLEEP_SECONDS="${SMOKE_SLEEP_SECONDS:-4}"

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

smoke_check() {
  local attempt
  local status

  for attempt in $(seq 1 "${MAX_SMOKE_ATTEMPTS}"); do
    status="$(curl -s -o /dev/null -w '%{http_code}' -H "Host: ${PROD_API_DOMAIN}" http://127.0.0.1/v1/content/trending-topics || true)"
    if [ "${status}" = "401" ]; then
      return 0
    fi
    sleep "${SMOKE_SLEEP_SECONDS}"
  done

  echo "User management smoke check failed"
  return 1
}
cd "${SOURCE_DIR}"
rm -rf target
git config --global --add safe.directory "${SOURCE_DIR}" || true
"${MAVEN_BIN}" -Dmaven.test.skip=true -Djava.version=21 clean package

RELEASE_SHA="$(git rev-parse HEAD)"
RELEASE_DIR="${APP_DIR}/releases/${RELEASE_SHA}"
CURRENT_DIR="${APP_DIR}/current"
JAR_PATH="$(find target -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"

if [ -z "${JAR_PATH}" ]; then
  echo "Built JAR not found"
  exit 1
fi

mkdir -p "${RELEASE_DIR}" "${CURRENT_DIR}"
cp "${JAR_PATH}" "${RELEASE_DIR}/app.jar"
ln -sfn "${RELEASE_DIR}/app.jar" "${CURRENT_DIR}/app.jar"
chown -R advocacy:advocacy "${APP_DIR}"

cat > "${CURRENT_DIR}/runtime.env" <<EOF
SERVER_PORT=${SERVER_PORT}
SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
EOF

# Helper: append env var to runtime.env if set
append_env() {
  local var_name="$1"
  local var_value="${!var_name:-}"
  if [ -n "${var_value}" ]; then
    echo "${var_name}=${var_value}" >> "${CURRENT_DIR}/runtime.env"
  fi
}

# Database
append_env DB_HOST
append_env DB_PORT
append_env DB_NAME
append_env DB_USER
append_env DB_PASSWORD

# JWT
append_env APP_JWT_SECRET
append_env APP_JWT_EXPIRATION_MS

# AI / Gemini
append_env APP_AI_GEMINI_API_KEY
append_env APP_AI_GEMINI_API_URL
append_env APP_AI_GEMINI_FLASH_API_URL

# OAuth
append_env APP_OAUTH_LINKEDIN_CLIENT_ID
append_env APP_OAUTH_LINKEDIN_ORG_CLIENT_ID
append_env APP_OAUTH_META_APP_ID

# CORS
append_env APP_CORS_ALLOWED_ORIGINS

# URLs
append_env APP_FRONTEND_BASE_URL
append_env APP_API_BASE_URL

# Proxy service URLs
append_env APP_PROXY_EXTERNAL_INGESTION_URL
append_env APP_PROXY_PRODUCTS_URL
append_env APP_PROXY_ORDERS_URL

# reCAPTCHA
append_env APP_SECURITY_RECAPTCHA_SECRET_KEY
append_env APP_SECURITY_RECAPTCHA_VERIFY_URL
append_env APP_SECURITY_RECAPTCHA_MIN_SCORE

systemctl daemon-reload
systemctl restart "${UNIT_NAME}"
systemctl is-active --quiet "${UNIT_NAME}"
smoke_check

if [ "${RELEASE_RETENTION}" -gt 0 ]; then
  ls -1dt "${APP_DIR}/releases"/* 2>/dev/null | tail -n +"$((RELEASE_RETENTION + 1))" | xargs -r rm -rf
fi

echo "Deployment completed for ${UNIT_NAME}"
