#!/bin/bash
set -euo pipefail

BACKUP_CONFIG="${BACKUP_CONFIG:-/etc/advocacy/db-backup.env}"
ALERT_SCRIPT="${ALERT_SCRIPT:-/usr/local/sbin/socialripple-send-email}"

if [ -f "${BACKUP_CONFIG}" ]; then
  # shellcheck disable=SC1090
  . "${BACKUP_CONFIG}"
fi

BACKUP_ROOT="${BACKUP_ROOT:-/var/backups/socialripple/mysql}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
DATABASES="${DATABASES:-advocacy_db Advocacy_dev}"
TIMESTAMP="$(date +%F-%H%M%S)"
BACKUP_DIR="${BACKUP_ROOT}/${TIMESTAMP}"

mkdir -p "${BACKUP_DIR}"

for db in ${DATABASES}; do
  dump_path="${BACKUP_DIR}/${db}.sql.gz"
  mysqldump --single-transaction --quick --routines --triggers "${db}" | gzip -9 > "${dump_path}"
done

find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -mtime +"$((BACKUP_RETENTION_DAYS - 1))" -exec rm -rf {} +

if [ "${ENABLE_DO_SPACES:-false}" = "true" ]; then
  if command -v aws >/dev/null 2>&1; then
    : "${DO_SPACES_BUCKET:?DO_SPACES_BUCKET is required when ENABLE_DO_SPACES=true}"
    : "${DO_SPACES_ENDPOINT:?DO_SPACES_ENDPOINT is required when ENABLE_DO_SPACES=true}"
    aws --endpoint-url "${DO_SPACES_ENDPOINT}" s3 cp "${BACKUP_DIR}" "s3://${DO_SPACES_BUCKET}/${TIMESTAMP}/" --recursive
  else
    "${ALERT_SCRIPT}" "[SocialRipple Prod] DB backup upload skipped" \
      "ENABLE_DO_SPACES is true but aws cli is not installed on $(hostname)." || true
  fi
fi

echo "Backup completed in ${BACKUP_DIR}"
