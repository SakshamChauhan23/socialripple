#!/bin/bash
set -euo pipefail

APP_FRONTEND_DOMAIN="${APP_FRONTEND_DOMAIN:-dashboard.socialripple.ai}"
APP_API_DOMAIN="${APP_API_DOMAIN:-api.socialripple.ai}"

echo "timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "hostname=$(hostname)"
echo

echo "[services]"
systemctl is-active \
  advocacy-user-management-prod.service \
  advocacy-publication-service-prod.service \
  advocacy-notification-service-prod.service \
  advocacy-media-service-prod.service
echo

echo "[ports]"
ss -ltnp | awk 'NR==1 || /:80 |:443 |:8080 |:8081 |:8082 |:8084 /'
echo

echo "[memory]"
free -h
echo

echo "[swap]"
swapon --show || true
echo

echo "[processes]"
ps -eo pid,ppid,cmd,%cpu,%mem,rss --sort=-%mem | awk 'NR==1 || /app\.jar|nginx: master|nginx: worker/'
echo

echo "[smoke]"
curl -sk --resolve "${APP_FRONTEND_DOMAIN}:443:127.0.0.1" -o /dev/null -w "ui=%{http_code}\n" "https://${APP_FRONTEND_DOMAIN}/"
curl -sk --resolve "${APP_API_DOMAIN}:443:127.0.0.1" -o /dev/null -w "v1=%{http_code}\n" "https://${APP_API_DOMAIN}/v1/content/trending-topics"
curl -sk --resolve "${APP_API_DOMAIN}:443:127.0.0.1" -o /dev/null -w "ingest=%{http_code}\n" "https://${APP_API_DOMAIN}/external-ingestion/"
curl -sk --resolve "${APP_API_DOMAIN}:443:127.0.0.1" -o /dev/null -w "media=%{http_code}\n" "https://${APP_API_DOMAIN}/api/media/content?file_id=test"
echo

echo "[loadavg]"
uptime
