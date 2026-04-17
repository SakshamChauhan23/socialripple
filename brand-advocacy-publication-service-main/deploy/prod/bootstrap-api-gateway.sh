#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

: "${PROD_API_DOMAIN:?PROD_API_DOMAIN is required}"
: "${USER_MANAGEMENT_PORT:=8081}"
: "${PUBLICATION_PORT:=8080}"
: "${NOTIFICATION_PORT:=8082}"
: "${MEDIA_PORT:=8084}"
: "${RUN_CERTBOT:=false}"

NGINX_SITES_AVAILABLE="/etc/nginx/sites-available"
NGINX_SITES_ENABLED="/etc/nginx/sites-enabled"
CONF_PATH="${NGINX_SITES_AVAILABLE}/${PROD_API_DOMAIN}.conf"
CERT_PATH="/etc/letsencrypt/live/${PROD_API_DOMAIN}/fullchain.pem"
CERT_KEY_PATH="/etc/letsencrypt/live/${PROD_API_DOMAIN}/privkey.pem"

write_http_only_conf() {
  cat > "${CONF_PATH}" <<EOF
server {
    listen 80;
    server_name ${PROD_API_DOMAIN};

    location /external-ingestion/ {
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

    location /api/media/ {
        proxy_pass http://127.0.0.1:${USER_MANAGEMENT_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
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
}

write_https_conf() {
  cat > "${CONF_PATH}" <<EOF
server {
    listen 80;
    server_name ${PROD_API_DOMAIN};

    location ~ /.well-known/acme-challenge {
        allow all;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name ${PROD_API_DOMAIN};

    ssl_certificate ${CERT_PATH};
    ssl_certificate_key ${CERT_KEY_PATH};
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    location /external-ingestion/ {
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

    location /api/media/ {
        proxy_pass http://127.0.0.1:${USER_MANAGEMENT_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
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
}
EOF
}

if ! command -v nginx >/dev/null 2>&1 || ! command -v certbot >/dev/null 2>&1; then
  apt-get update
  apt-get install -y nginx certbot python3-certbot-nginx
fi

if [ -f "${CERT_PATH}" ] && [ -f "${CERT_KEY_PATH}" ]; then
  write_https_conf
else
  write_http_only_conf
fi

ln -sfn "${CONF_PATH}" "${NGINX_SITES_ENABLED}/${PROD_API_DOMAIN}.conf"
nginx -t
systemctl reload nginx

if [ "${RUN_CERTBOT}" = "true" ]; then
  certbot --nginx --non-interactive --agree-tos --redirect \
    -m "devops@moonhive.in" \
    -d "${PROD_API_DOMAIN}"

  if [ -f "${CERT_PATH}" ] && [ -f "${CERT_KEY_PATH}" ]; then
    write_https_conf
    nginx -t
    systemctl reload nginx
  fi
else
  echo "Skipping certbot issuance for ${PROD_API_DOMAIN}; set RUN_CERTBOT=true to request or renew via this script."
fi

systemctl enable nginx
