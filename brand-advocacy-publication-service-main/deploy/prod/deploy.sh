#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

SOURCE_DIR="/github/prod/publication-service"
APP_DIR="/opt/advocacy/publication-service-prod"
UNIT_NAME="advocacy-publication-service-prod.service"
SERVER_PORT="${SERVER_PORT:-8080}"
SPRING_PROFILE="prod"
JAVA_HOME="/opt/jdk-21"
PROD_API_DOMAIN="${PROD_API_DOMAIN:-api.socialripple.ai}"
RELEASE_RETENTION="${RELEASE_RETENTION:-5}"
MAX_SMOKE_ATTEMPTS="${MAX_SMOKE_ATTEMPTS:-15}"
SMOKE_SLEEP_SECONDS="${SMOKE_SLEEP_SECONDS:-3}"

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

: "${APP_FRONTEND_BASE_URL:?APP_FRONTEND_BASE_URL is required}"
: "${APP_API_BASE_URL:?APP_API_BASE_URL is required}"

append_env() {
  local var_name="$1"
  local var_value="${!var_name:-}"
  if [ -n "${var_value}" ]; then
    echo "${var_name}=${var_value}" >> "${CURRENT_DIR}/runtime.env"
  fi
}

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

cd "${SOURCE_DIR}"
rm -rf target
git config --global --add safe.directory "${SOURCE_DIR}" || true
bash ./mvnw -Dmaven.test.skip=true clean package

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
EOF

append_env DB_HOST
append_env DB_PORT
append_env DB_NAME
append_env DB_USER
append_env DB_PASSWORD
append_env APP_CORS_ALLOWED_ORIGINS
append_env APP_OAUTH_X_CONSUMER_KEY
append_env APP_OAUTH_X_CONSUMER_SECRET
append_env APP_OAUTH_META_APP_ID
append_env APP_OAUTH_META_APP_SECRET
append_env APP_OAUTH_LINKEDIN_CLIENT_ID
append_env APP_OAUTH_LINKEDIN_CLIENT_SECRET
append_env APP_OAUTH_LINKEDIN_SCOPE
append_env APP_OAUTH_LINKEDIN_ORG_CLIENT_ID
append_env APP_OAUTH_LINKEDIN_ORG_CLIENT_SECRET
append_env APP_FRONTEND_BASE_URL
append_env APP_API_BASE_URL

# DB migrations
MIGRATION_DB_HOST="${DB_HOST:?DB_HOST is required}"
MIGRATION_DB_PORT="${DB_PORT:-3306}"
MIGRATION_DB_NAME="${DB_NAME:?DB_NAME is required}"
MIGRATION_DB_USER="${DB_USER:?DB_USER is required}"
MIGRATION_DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"

if ! command -v mysql >/dev/null 2>&1; then
  apt-get update -qq && apt-get install -y -qq default-mysql-client
fi

mysql_exec() {
  MYSQL_PWD="${MIGRATION_DB_PASSWORD}" mysql \
    --protocol=TCP \
    -h "${MIGRATION_DB_HOST}" \
    -P "${MIGRATION_DB_PORT}" \
    -u "${MIGRATION_DB_USER}" \
    "${MIGRATION_DB_NAME}" "$@"
}

mysql_exec -e "
CREATE TABLE IF NOT EXISTS share_analytics (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  external_share_id BIGINT NOT NULL,
  platform VARCHAR(20) NOT NULL,
  tenant_id BIGINT NOT NULL,
  impressions INT NOT NULL DEFAULT 0,
  reach INT NOT NULL DEFAULT 0,
  likes INT NOT NULL DEFAULT 0,
  comments INT NOT NULL DEFAULT 0,
  shares INT NOT NULL DEFAULT 0,
  saves INT NOT NULL DEFAULT 0,
  bookmarks INT NOT NULL DEFAULT 0,
  retweets INT NOT NULL DEFAULT 0,
  replies INT NOT NULL DEFAULT 0,
  quotes INT NOT NULL DEFAULT 0,
  clicks INT NOT NULL DEFAULT 0,
  video_views INT NOT NULL DEFAULT 0,
  engagements INT NOT NULL DEFAULT 0,
  fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_share_analytics_share (external_share_id),
  INDEX idx_share_analytics_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
"

systemctl daemon-reload
systemctl restart "${UNIT_NAME}"
systemctl is-active --quiet "${UNIT_NAME}"
smoke_check

if [ "${RELEASE_RETENTION}" -gt 0 ]; then
  ls -1dt "${APP_DIR}/releases"/* 2>/dev/null | tail -n +"$((RELEASE_RETENTION + 1))" | xargs -r rm -rf
fi

echo "Deployment completed for ${UNIT_NAME}"
