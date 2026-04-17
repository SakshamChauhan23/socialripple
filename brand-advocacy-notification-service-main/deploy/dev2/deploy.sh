#!/bin/bash
set -euo pipefail

SOURCE_DIR="/github/dev2/notification-service"
APP_DIR="/opt/advocacy/notification-service-dev2"
UNIT_NAME="advocacy-notification-service-dev2.service"
SERVER_PORT="9082"
SPRING_PROFILE="dev"
JAVA_HOME="/opt/jdk-21"

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

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
EOF

systemctl daemon-reload
systemctl restart "${UNIT_NAME}"
systemctl is-active --quiet "${UNIT_NAME}"

echo "Deployment completed for ${UNIT_NAME}"
