#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

OPS_SOURCE_DIR="${1:-/github/prod/publication-service/deploy/prod/ops}"
OPS_BIN_DIR="/usr/local/sbin"
ALERT_CONFIG="/etc/advocacy/ops-alert.env"
BACKUP_CONFIG="/etc/advocacy/db-backup.env"
CRON_FILE="/etc/cron.d/socialripple-db-ops"

mkdir -p /etc/advocacy /var/backups/socialripple/mysql

install -m 755 "${OPS_SOURCE_DIR}/send-email.sh" "${OPS_BIN_DIR}/socialripple-send-email"
install -m 755 "${OPS_SOURCE_DIR}/db-health-check.sh" "${OPS_BIN_DIR}/socialripple-db-health-check"
install -m 755 "${OPS_SOURCE_DIR}/db-backup.sh" "${OPS_BIN_DIR}/socialripple-db-backup"
install -m 755 "${OPS_SOURCE_DIR}/db-restore-validate.sh" "${OPS_BIN_DIR}/socialripple-db-restore-validate"
install -m 755 "${OPS_SOURCE_DIR}/db-performance-snapshot.sh" "${OPS_BIN_DIR}/socialripple-db-performance-snapshot"
install -m 755 "${OPS_SOURCE_DIR}/enable-slow-query-log.sh" "${OPS_BIN_DIR}/socialripple-enable-slow-query-log"
install -m 755 "${OPS_SOURCE_DIR}/fetch-latest-login-session.sh" "${OPS_BIN_DIR}/socialripple-fetch-latest-login-session"

if [ ! -f "${ALERT_CONFIG}" ]; then
  cat > "${ALERT_CONFIG}" <<'EOF'
ALERT_EMAIL_PROVIDER=log
ALERT_EMAIL_TO=devops@moonhive.in
ALERT_EMAIL_FROM=socialripple-alerts@localhost
# For HTTPS email providers, set one of:
# ALERT_EMAIL_PROVIDER=sendgrid
# ALERT_EMAIL_PROVIDER=resend
# ALERT_EMAIL_PROVIDER=mailgun
# ALERT_EMAIL_API_KEY=
# ALERT_EMAIL_MAILGUN_DOMAIN=
EOF
  chmod 600 "${ALERT_CONFIG}"
fi

if [ ! -f "${BACKUP_CONFIG}" ]; then
  cat > "${BACKUP_CONFIG}" <<'EOF'
BACKUP_ROOT=/var/backups/socialripple/mysql
BACKUP_RETENTION_DAYS=7
DATABASES="advocacy_db Advocacy_dev"
ENABLE_DO_SPACES=false
# DO Spaces placeholders for future enablement:
# DO_SPACES_BUCKET=
# DO_SPACES_ENDPOINT=
# AWS_ACCESS_KEY_ID=
# AWS_SECRET_ACCESS_KEY=
EOF
  chmod 600 "${BACKUP_CONFIG}"
fi

cat > "${CRON_FILE}" <<'EOF'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
*/10 * * * * root /usr/local/sbin/socialripple-db-health-check >> /var/log/socialripple-db-health.log 2>&1
15 2 * * * root /usr/local/sbin/socialripple-db-backup >> /var/log/socialripple-db-backup.log 2>&1
0 8 * * * root /usr/local/sbin/socialripple-db-health-check summary >> /var/log/socialripple-db-health.log 2>&1
0 3 1 * * root /usr/local/sbin/socialripple-db-restore-validate >> /var/log/socialripple-db-backup.log 2>&1
EOF

chmod 644 "${CRON_FILE}"
touch /var/log/socialripple-db-health.log /var/log/socialripple-db-backup.log

echo "Installed DB ops scripts and cron jobs"
