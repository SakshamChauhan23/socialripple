#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

SOURCE_DIR="/github/dev2/user-management"
APP_DIR="/opt/advocacy/user-management-dev2"
UNIT_NAME="advocacy-user-management-dev2.service"
SERVER_PORT="9081"
SPRING_PROFILE="dev"
JAVA_HOME="/opt/jdk-21"
MAVEN_BIN="/opt/apache-maven/bin/mvn"

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

: "${APP_FRONTEND_BASE_URL:?APP_FRONTEND_BASE_URL is required}"
: "${APP_API_BASE_URL:?APP_API_BASE_URL is required}"

append_env_if_present() {
  local key="$1"
  local value="${!key:-}"
  if [ -n "${value}" ]; then
    cat >> "${CURRENT_DIR}/runtime.env" <<EOF
${key}=${value}
EOF
  fi
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
APP_FRONTEND_BASE_URL=${APP_FRONTEND_BASE_URL}
APP_API_BASE_URL=${APP_API_BASE_URL}
APP_SECURITY_CAPTCHA_ENABLED=false
EOF

append_env_if_present DB_HOST
append_env_if_present DB_PORT
append_env_if_present DB_NAME
append_env_if_present DB_USER
append_env_if_present DB_PASSWORD
append_env_if_present APP_OAUTH_LINKEDIN_CLIENT_ID
append_env_if_present APP_OAUTH_LINKEDIN_ORG_CLIENT_ID
append_env_if_present APP_OAUTH_LINKEDIN_CALLBACK_URL
append_env_if_present APP_OAUTH_FACEBOOK_CALLBACK_URL
append_env_if_present APP_OAUTH_INSTAGRAM_CALLBACK_URL
append_env_if_present APP_CORS_ALLOWED_ORIGINS

SYSTEM_ENV_FILE="/etc/advocacy/user-management-dev2.env"
if [ -f "${SYSTEM_ENV_FILE}" ]; then
  sed -i '/^APP_SECURITY_CAPTCHA_ENABLED=/d' "${SYSTEM_ENV_FILE}"
  echo "APP_SECURITY_CAPTCHA_ENABLED=false" >> "${SYSTEM_ENV_FILE}"
fi

if [ -n "${APP_AI_GEMINI_API_KEY:-}" ]; then
cat >> "${CURRENT_DIR}/runtime.env" <<EOF
APP_AI_GEMINI_API_KEY=${APP_AI_GEMINI_API_KEY}
EOF
fi

if [ -n "${APP_AI_GEMINI_API_URL:-}" ]; then
cat >> "${CURRENT_DIR}/runtime.env" <<EOF
APP_AI_GEMINI_API_URL=${APP_AI_GEMINI_API_URL}
EOF
fi

if [ -n "${APP_AI_GEMINI_FLASH_API_URL:-}" ]; then
cat >> "${CURRENT_DIR}/runtime.env" <<EOF
APP_AI_GEMINI_FLASH_API_URL=${APP_AI_GEMINI_FLASH_API_URL}
EOF
fi

if [ -n "${APP_SECURITY_RECAPTCHA_SECRET_KEY:-}" ]; then
cat >> "${CURRENT_DIR}/runtime.env" <<EOF
APP_SECURITY_RECAPTCHA_SECRET_KEY=${APP_SECURITY_RECAPTCHA_SECRET_KEY}
EOF
fi

if [ -n "${APP_SECURITY_RECAPTCHA_VERIFY_URL:-}" ]; then
cat >> "${CURRENT_DIR}/runtime.env" <<EOF
APP_SECURITY_RECAPTCHA_VERIFY_URL=${APP_SECURITY_RECAPTCHA_VERIFY_URL}
EOF
fi

if [ -n "${APP_SECURITY_RECAPTCHA_MIN_SCORE:-}" ]; then
cat >> "${CURRENT_DIR}/runtime.env" <<EOF
APP_SECURITY_RECAPTCHA_MIN_SCORE=${APP_SECURITY_RECAPTCHA_MIN_SCORE}
EOF
fi

MIGRATION_DB_HOST="${DB_HOST:?DB_HOST is required}"
MIGRATION_DB_PORT="${DB_PORT:-3306}"
MIGRATION_DB_NAME="${DB_NAME:?DB_NAME is required}"
MIGRATION_DB_USER="${DB_USER:?DB_USER is required}"
MIGRATION_DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"

if ! command -v mysql >/dev/null 2>&1; then
  apt-get update
  apt-get install -y default-mysql-client
fi

mysql_exec() {
  MYSQL_PWD="${MIGRATION_DB_PASSWORD}" mysql \
    --protocol=TCP \
    -h "${MIGRATION_DB_HOST}" \
    -P "${MIGRATION_DB_PORT}" \
    -u "${MIGRATION_DB_USER}" \
    "${MIGRATION_DB_NAME}" "$@"
}

ensure_user_auth_token_column() {
  local column_name="$1"
  local definition="$2"
  local column_count

  column_count="$(mysql_exec -Nse "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${MIGRATION_DB_NAME}' AND TABLE_NAME='user_auth_token' AND COLUMN_NAME='${column_name}'")"
  if [ "${column_count}" = "0" ]; then
    mysql_exec -e "ALTER TABLE user_auth_token ADD COLUMN ${definition};"
  fi
}

ensure_user_auth_token_column "oauth_source_page" "oauth_source_page VARCHAR(32) NULL DEFAULT NULL"

# Apply targeted seed migrations. Listed by name (not a wildcard) so older
# placeholder migrations like 20260327_seed_bunny_media_config.sql are NOT
# re-run on every deploy - that file overwrites BUNNY_STORAGE_ZONE with
# REPLACE_ME unconditionally and would corrupt production config. Only files
# explicitly listed here run, and they MUST be idempotent UPSERTs that ship
# the actual production value (not placeholders).
NEWSAPI_SEED="${SOURCE_DIR}/deploy/dev2/sql/20260408_seed_newsapi_config.sql"
if [ -f "${NEWSAPI_SEED}" ]; then
  echo "Applying seed migration: 20260408_seed_newsapi_config.sql"
  mysql_exec < "${NEWSAPI_SEED}"
fi

systemctl daemon-reload
systemctl restart "${UNIT_NAME}"
systemctl is-active --quiet "${UNIT_NAME}"

echo "Deployment completed for ${UNIT_NAME}"
