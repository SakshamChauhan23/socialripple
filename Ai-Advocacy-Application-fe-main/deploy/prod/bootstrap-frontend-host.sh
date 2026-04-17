#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

: "${PROD_FRONTEND_DOMAIN:?PROD_FRONTEND_DOMAIN is required}"

WEBROOT_BASE="${PROD_FE_WEBROOT:-/var/www/${PROD_FRONTEND_DOMAIN}}"
CURRENT_WEBROOT="${WEBROOT_BASE}/current"
NGINX_SITES_AVAILABLE="/etc/nginx/sites-available"
NGINX_SITES_ENABLED="/etc/nginx/sites-enabled"
CONF_PATH="${NGINX_SITES_AVAILABLE}/${PROD_FRONTEND_DOMAIN}.conf"

if ! command -v nginx >/dev/null 2>&1 || ! command -v certbot >/dev/null 2>&1; then
  apt-get update
  apt-get install -y nginx certbot python3-certbot-nginx
fi

mkdir -p "${WEBROOT_BASE}/releases" "${CURRENT_WEBROOT}"

cat > "${CONF_PATH}" <<EOF
server {
    listen 80;
    server_name ${PROD_FRONTEND_DOMAIN};

    root ${CURRENT_WEBROOT};
    index index.html;

    location / {
        try_files \$uri /index.html;
    }

    location /static/ {
        expires 7d;
        add_header Cache-Control "public, max-age=604800, immutable";
    }

    location ~ /.well-known/acme-challenge {
        allow all;
    }
}
EOF

ln -sfn "${CONF_PATH}" "${NGINX_SITES_ENABLED}/${PROD_FRONTEND_DOMAIN}.conf"
nginx -t
systemctl reload nginx

certbot --nginx --non-interactive --agree-tos --redirect \
  -m "devops@moonhive.in" \
  -d "${PROD_FRONTEND_DOMAIN}"

systemctl enable nginx
