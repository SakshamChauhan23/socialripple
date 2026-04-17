#!/bin/bash
set -euo pipefail

SOURCE_DIR="/github/dev2/publication-service"
APP_DIR="/opt/advocacy/publication-service-dev2"
UNIT_NAME="advocacy-publication-service-dev2.service"
SERVER_PORT="9080"
SPRING_PROFILE="dev"
JAVA_HOME="/opt/jdk-21"
READINESS_PATH="/v1/api/x/auth"

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
./mvnw -Dmaven.test.skip=true clean package

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

append_env_if_present DB_HOST
append_env_if_present DB_PORT
append_env_if_present DB_NAME
append_env_if_present DB_USER
append_env_if_present DB_PASSWORD
append_env_if_present APP_OAUTH_LINKEDIN_CLIENT_ID
append_env_if_present APP_OAUTH_LINKEDIN_CLIENT_SECRET
append_env_if_present APP_OAUTH_LINKEDIN_CALLBACK_URL
append_env_if_present APP_OAUTH_LINKEDIN_SCOPE
append_env_if_present APP_OAUTH_META_APP_ID
append_env_if_present APP_OAUTH_META_APP_SECRET
append_env_if_present APP_OAUTH_FACEBOOK_CALLBACK_URL
append_env_if_present APP_OAUTH_INSTAGRAM_CALLBACK_URL
append_env_if_present APP_OAUTH_ORG_LINKEDIN_CALLBACK_URL
append_env_if_present APP_OAUTH_ORG_X_CALLBACK_URL
append_env_if_present APP_OAUTH_X_CONSUMER_KEY
append_env_if_present APP_OAUTH_X_CONSUMER_SECRET
append_env_if_present APP_OAUTH_LINKEDIN_ORG_CLIENT_ID
append_env_if_present APP_OAUTH_LINKEDIN_ORG_CLIENT_SECRET
append_env_if_present APP_CORS_ALLOWED_ORIGINS

wait_for_service_ready() {
  local attempt
  local max_attempts=45
  local status

  for attempt in $(seq 1 "${max_attempts}"); do
    status="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${SERVER_PORT}${READINESS_PATH}" || true)"
    case "${status}" in
      200|204|400|401|403|404)
        echo "publication-service readiness probe returned ${status} on attempt ${attempt}/${max_attempts}"
        return 0
        ;;
    esac

    echo "Waiting for ${UNIT_NAME} on 127.0.0.1:${SERVER_PORT}${READINESS_PATH} (attempt ${attempt}/${max_attempts}, status=${status:-none})"
    sleep 2
  done

  echo "publication-service did not become ready on ${READINESS_PATH}"
  return 1
}

MIGRATION_DB_HOST="${DB_HOST:-206.189.130.17}"
MIGRATION_DB_PORT="${DB_PORT:-3306}"
MIGRATION_DB_NAME="${DB_NAME:-advocacy_db}"
MIGRATION_DB_USER="${DB_USER:-qa-user}"
MIGRATION_DB_PASSWORD="${DB_PASSWORD:-Wr@345#bfdqsdfytytDF}"

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

mysql_exec -e "
CREATE TABLE IF NOT EXISTS org_business_connect_tx (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  transaction_id VARCHAR(255) NOT NULL UNIQUE,
  organization_id BIGINT NOT NULL,
  initiated_by_user_id BIGINT NOT NULL,
  platform VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  temporary_access_token TEXT NULL,
  temporary_refresh_token TEXT NULL,
  temporary_access_secret TEXT NULL,
  provider_user_id VARCHAR(255) NULL,
  provider_username VARCHAR(255) NULL,
  provider_display_name VARCHAR(512) NULL,
  discovered_pages_json LONGTEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
"

ensure_external_platform_column() {
  local column_name="$1"
  local definition="$2"
  local column_count

  column_count="$(mysql_exec -Nse "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${MIGRATION_DB_NAME}' AND TABLE_NAME='external_platforms' AND COLUMN_NAME='${column_name}'")"
  if [ "${column_count}" = "0" ]; then
    mysql_exec -e "ALTER TABLE external_platforms ADD COLUMN ${definition};"
  fi
}

ensure_external_platform_column "last_sync_attempt_at" "last_sync_attempt_at TIMESTAMP NULL DEFAULT NULL"
ensure_external_platform_column "last_sync_success_at" "last_sync_success_at TIMESTAMP NULL DEFAULT NULL"
ensure_external_platform_column "last_sync_status" "last_sync_status VARCHAR(50) NULL DEFAULT NULL"
ensure_external_platform_column "last_sync_error" "last_sync_error TEXT NULL"
ensure_external_platform_column "last_sync_error_at" "last_sync_error_at TIMESTAMP NULL DEFAULT NULL"
ensure_external_platform_column "last_imported_count" "last_imported_count INT NULL DEFAULT 0"

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
ensure_user_auth_token_column "last_sync_attempt_at" "last_sync_attempt_at TIMESTAMP NULL DEFAULT NULL"
ensure_user_auth_token_column "last_sync_success_at" "last_sync_success_at TIMESTAMP NULL DEFAULT NULL"
ensure_user_auth_token_column "last_sync_status" "last_sync_status VARCHAR(50) NULL DEFAULT NULL"
ensure_user_auth_token_column "last_sync_error" "last_sync_error TEXT NULL"
ensure_user_auth_token_column "last_sync_error_at" "last_sync_error_at TIMESTAMP NULL DEFAULT NULL"

mysql_exec -e "ALTER TABLE posts CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

ensure_posts_column_charset() {
  local column_name="$1"
  local data_type="$2"
  local is_nullable="$3"
  local column_count
  local charset_name
  local collation_name

  column_count="$(mysql_exec -Nse "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${MIGRATION_DB_NAME}' AND TABLE_NAME='posts' AND COLUMN_NAME='${column_name}'")"
  if [ "${column_count}" = "0" ]; then
    return
  fi

  charset_name="$(mysql_exec -Nse "SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${MIGRATION_DB_NAME}' AND TABLE_NAME='posts' AND COLUMN_NAME='${column_name}'")"
  collation_name="$(mysql_exec -Nse "SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${MIGRATION_DB_NAME}' AND TABLE_NAME='posts' AND COLUMN_NAME='${column_name}'")"

  if [ "${charset_name}" != "utf8mb4" ] || [ "${collation_name}" != "utf8mb4_unicode_ci" ]; then
    mysql_exec -e "ALTER TABLE posts MODIFY COLUMN ${column_name} ${data_type} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ${is_nullable};"
  fi
}

ensure_posts_column_charset "content" "TEXT" "NULL"
ensure_posts_column_charset "title" "TEXT" "NULL"
ensure_posts_column_charset "x_generated_content" "TEXT" "NULL"

ensure_posts_column() {
  local column_name="$1"
  local definition="$2"
  local column_count

  column_count="$(mysql_exec -Nse "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${MIGRATION_DB_NAME}' AND TABLE_NAME='posts' AND COLUMN_NAME='${column_name}'")"
  if [ "${column_count}" = "0" ]; then
    mysql_exec -e "ALTER TABLE posts ADD COLUMN ${definition};"
  fi
}

ensure_posts_column "source_name" "source_name VARCHAR(255) NULL DEFAULT NULL"
ensure_posts_column "source_username" "source_username VARCHAR(255) NULL DEFAULT NULL"
ensure_posts_column "source_avatar_url" "source_avatar_url VARCHAR(2000) NULL DEFAULT NULL"

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
wait_for_service_ready

echo "Deployment completed for ${UNIT_NAME}"
