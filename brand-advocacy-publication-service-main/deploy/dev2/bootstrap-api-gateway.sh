#!/bin/bash
set -euo pipefail

: "${DEV2_API_DOMAIN:?DEV2_API_DOMAIN is required}"
: "${USER_MANAGEMENT_PORT:=9081}"
: "${PUBLICATION_PORT:=9080}"
: "${NOTIFICATION_PORT:=9082}"
: "${MEDIA_PORT:=9084}"

NGINX_SITES_AVAILABLE="/etc/nginx/sites-available"
NGINX_SITES_ENABLED="/etc/nginx/sites-enabled"
CONF_PATH="${NGINX_SITES_AVAILABLE}/${DEV2_API_DOMAIN}.conf"
LETSENCRYPT_LIVE_DIR="/etc/letsencrypt/live/${DEV2_API_DOMAIN}"

wait_for_publication_port() {
  local attempt
  local max_attempts=30
  local status

  for attempt in $(seq 1 "${max_attempts}"); do
    status="$(curl -s -o /dev/null -w '%{http_code}' \
      "http://127.0.0.1:${PUBLICATION_PORT}/v1/api/x/auth" || true)"
    if [ "${status}" != "000" ]; then
      return 0
    fi

    echo "Waiting for publication-service on 127.0.0.1:${PUBLICATION_PORT} (attempt ${attempt}/${max_attempts})"
    sleep 2
  done

  return 1
}

certbot_is_busy() {
  pgrep -f "[c]ertbot" >/dev/null 2>&1
}

certificate_exists() {
  [ -f "${LETSENCRYPT_LIVE_DIR}/fullchain.pem" ] && [ -f "${LETSENCRYPT_LIVE_DIR}/privkey.pem" ]
}

wait_for_certbot_idle() {
  local attempt
  local max_attempts=30

  for attempt in $(seq 1 "${max_attempts}"); do
    if ! certbot_is_busy; then
      return 0
    fi

    echo "Waiting for existing Certbot process to finish (attempt ${attempt}/${max_attempts})"
    sleep 5
  done

  return 1
}

ensure_certificate() {
  if certificate_exists; then
    echo "Certificate already exists for ${DEV2_API_DOMAIN}; skipping Certbot"
    return 0
  fi

  if ! wait_for_certbot_idle; then
    echo "Timed out waiting for existing Certbot process to finish"
    return 1
  fi

  if certificate_exists; then
    echo "Certificate became available for ${DEV2_API_DOMAIN}; skipping Certbot"
    return 0
  fi

  if certbot --nginx --non-interactive --agree-tos --redirect \
    -m "devops@moonhive.in" \
    -d "${DEV2_API_DOMAIN}"; then
    return 0
  fi

  if certbot_is_busy; then
    echo "Certbot lock detected while requesting certificate for ${DEV2_API_DOMAIN}; waiting for completion"
    if wait_for_certbot_idle && certificate_exists; then
      echo "Certificate was created by another Certbot process for ${DEV2_API_DOMAIN}"
      return 0
    fi
  fi

  echo "Certbot did not complete successfully for ${DEV2_API_DOMAIN}"
  return 1
}

if ! command -v nginx >/dev/null 2>&1 || ! command -v certbot >/dev/null 2>&1; then
  apt-get update
  apt-get install -y nginx certbot python3-certbot-nginx
fi

if ! wait_for_publication_port; then
  echo "Could not reach publication-service on 127.0.0.1:${PUBLICATION_PORT}"
  status="$(curl -s -o /dev/null -w '%{http_code}' \
    "http://127.0.0.1:${PUBLICATION_PORT}/v1/api/x/auth" || true)"
  echo "  ${PUBLICATION_PORT} -> ${status}"
  exit 1
fi

echo "Using publication-service upstream port ${PUBLICATION_PORT}"

if [ -f "${CONF_PATH}" ] && [ -f "${NGINX_SITES_ENABLED}/${DEV2_API_DOMAIN}.conf" ] && certificate_exists; then
  echo "Nginx config and certificate already exist for ${DEV2_API_DOMAIN}; skipping gateway setup"
  nginx -t
  systemctl reload nginx
  exit 0
fi

echo "First-time gateway setup for ${DEV2_API_DOMAIN}"

cat > "${CONF_PATH}" <<EOF
server {
    listen 80;
    server_name ${DEV2_API_DOMAIN};

    location /external-ingestion/ {
        if (\$request_method = OPTIONS) {
            add_header Access-Control-Allow-Origin \$http_origin always;
            add_header Access-Control-Allow-Methods "GET, POST, PUT, PATCH, DELETE, OPTIONS" always;
            add_header Access-Control-Allow-Headers "\$http_access_control_request_headers" always;
            add_header Access-Control-Allow-Credentials "true" always;
            add_header Access-Control-Max-Age 3600 always;
            add_header Vary "Origin,Access-Control-Request-Method,Access-Control-Request-Headers" always;
            return 204;
        }

        add_header Access-Control-Allow-Origin \$http_origin always;
        add_header Access-Control-Allow-Credentials "true" always;
        add_header Access-Control-Expose-Headers "*" always;
        add_header Vary "Origin,Access-Control-Request-Method,Access-Control-Request-Headers" always;
        proxy_hide_header Access-Control-Allow-Origin;
        proxy_hide_header Access-Control-Allow-Credentials;
        proxy_hide_header Access-Control-Expose-Headers;
        proxy_hide_header Vary;
        proxy_pass http://127.0.0.1:${PUBLICATION_PORT}/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /v1/ {
        proxy_pass http://127.0.0.1:${USER_MANAGEMENT_PORT};
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /ws {
        proxy_pass http://127.0.0.1:${USER_MANAGEMENT_PORT};
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
    }

    location /notifications/ {
        proxy_pass http://127.0.0.1:${NOTIFICATION_PORT}/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /media/ {
        proxy_pass http://127.0.0.1:${MEDIA_PORT}/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location ~ /.well-known/acme-challenge {
        allow all;
    }
}
EOF

ln -sfn "${CONF_PATH}" "${NGINX_SITES_ENABLED}/${DEV2_API_DOMAIN}.conf"
nginx -t
systemctl reload nginx
ensure_certificate
systemctl enable nginx
