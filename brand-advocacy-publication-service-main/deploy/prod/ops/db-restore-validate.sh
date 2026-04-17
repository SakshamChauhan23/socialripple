#!/bin/bash
set -euo pipefail

BACKUP_CONFIG="${BACKUP_CONFIG:-/etc/advocacy/db-backup.env}"
ALERT_SCRIPT="${ALERT_SCRIPT:-/usr/local/sbin/socialripple-send-email}"

if [ -f "${BACKUP_CONFIG}" ]; then
  # shellcheck disable=SC1090
  . "${BACKUP_CONFIG}"
fi

BACKUP_ROOT="${BACKUP_ROOT:-/var/backups/socialripple/mysql}"
VALIDATE_PREFIX="${VALIDATE_PREFIX:-restore_validation}"
latest_dir="$(ls -1dt "${BACKUP_ROOT}"/* 2>/dev/null | head -n 1)"

if [ -z "${latest_dir:-}" ] || [ ! -d "${latest_dir}" ]; then
  echo "No backup directory found in ${BACKUP_ROOT}" >&2
  exit 1
fi

for dump in "${latest_dir}"/*.sql.gz; do
  [ -e "${dump}" ] || continue
  db_name="$(basename "${dump}" .sql.gz)"
  validate_db="${VALIDATE_PREFIX}_${db_name}"
  mysql -e "DROP DATABASE IF EXISTS \`${validate_db}\`; CREATE DATABASE \`${validate_db}\`;"
  gunzip -c "${dump}" | sed "s/CREATE DATABASE.*;/CREATE DATABASE IF NOT EXISTS \`${validate_db}\`;/; s/USE \`\\{0,1\\}${db_name}\`\\{0,1\\};/USE \`${validate_db}\`;/g" | mysql "${validate_db}"
  table_count="$(mysql -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${validate_db}'")"
  if [ "${table_count}" -eq 0 ]; then
    "${ALERT_SCRIPT}" "[SocialRipple Prod] DB restore validation failed" \
      "Restore validation for ${db_name} created no tables from backup ${dump}." || true
    mysql -e "DROP DATABASE IF EXISTS \`${validate_db}\`;"
    exit 1
  fi
  mysql -e "DROP DATABASE IF EXISTS \`${validate_db}\`;"
done

echo "Restore validation completed for ${latest_dir}"
