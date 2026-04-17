#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

: "${REACT_APP_RECAPTCHA_SITE_KEY:?REACT_APP_RECAPTCHA_SITE_KEY is required}"

# Navigate to the project directory
cd /github/advocacy-fe/

# Remove node_modules and package-lock.json
rm -rf node_modules package-lock.json

cat > .env.production.local <<EOF
REACT_APP_RECAPTCHA_SITE_KEY=${REACT_APP_RECAPTCHA_SITE_KEY}
EOF

# Install dependencies with legacy peer deps
npm install

# Build the React project
npm run build

# Clean the deployment directory
cd /var/www/advocacy.moonhive-server.in.net/html/
rm -rf *

# Copy the build output to the deployment directory
cd /github/advocacy-fe/build/
scp -r ./* /var/www/advocacy.moonhive-server.in.net/html/
