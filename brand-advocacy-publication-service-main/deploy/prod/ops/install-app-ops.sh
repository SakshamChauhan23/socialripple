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
PERF_BASELINE_CONFIG="/etc/advocacy/perf-baseline.env"
CRON_FILE="/etc/cron.d/socialripple-app-ops"

mkdir -p /etc/advocacy

install -m 755 "${OPS_SOURCE_DIR}/send-email.sh" "${OPS_BIN_DIR}/socialripple-send-email"
install -m 755 "${OPS_SOURCE_DIR}/app-health-check.sh" "${OPS_BIN_DIR}/socialripple-app-health-check"
install -m 755 "${OPS_SOURCE_DIR}/app-performance-snapshot.sh" "${OPS_BIN_DIR}/socialripple-app-performance-snapshot"
install -m 755 "${OPS_SOURCE_DIR}/api-load-probe.sh" "${OPS_BIN_DIR}/socialripple-api-load-probe"
install -m 755 "${OPS_SOURCE_DIR}/run-performance-baseline.sh" "${OPS_BIN_DIR}/socialripple-run-performance-baseline"

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

if [ ! -f "${PERF_BASELINE_CONFIG}" ]; then
  cat > "${PERF_BASELINE_CONFIG}" <<'EOF'
# existing_jwt is the default. login_flow is the fallback mode if no active session is available.
PERF_BASELINE_AUTH_MODE=existing_jwt
PERF_BASELINE_AUTH_TOKEN=
PERF_BASELINE_TENANT_ID=
PERF_BASELINE_TEST_USERNAME=
PERF_BASELINE_AUTH_EXPIRES_AT=
# Optional fallback values for login_flow mode:
PERF_BASELINE_TEST_PASSWORD=
PERF_BASELINE_TEST_CAPTCHA_TOKEN=
EOF
  chmod 600 "${PERF_BASELINE_CONFIG}"
fi

cat > "${CRON_FILE}" <<'EOF'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
*/10 * * * * root /usr/local/sbin/socialripple-app-health-check >> /var/log/socialripple-app-health.log 2>&1
0 8 * * * root /usr/local/sbin/socialripple-app-health-check summary >> /var/log/socialripple-app-health.log 2>&1
EOF

chmod 644 "${CRON_FILE}"
touch /var/log/socialripple-app-health.log

echo "Installed app ops scripts and cron jobs"
