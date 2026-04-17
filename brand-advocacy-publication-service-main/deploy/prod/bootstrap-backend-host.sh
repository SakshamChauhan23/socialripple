#!/bin/bash
set -euo pipefail

require_ci_deploy() {
  if [ "${SOCIALRIPPLE_CI_DEPLOY:-}" != "1" ]; then
    echo "Manual SSH deployment is disabled. Use the repository's GitHub Actions workflow."
    exit 1
  fi
}

require_ci_deploy

JAVA21_DIR="/opt/jdk-21"
MAVEN_DIR="/opt/apache-maven"
MAVEN_VERSION="3.9.10"

if ! id advocacy >/dev/null 2>&1; then
  useradd --system --create-home --shell /usr/sbin/nologin advocacy
fi

if ! command -v java >/dev/null 2>&1; then
  apt-get update
  apt-get install -y openjdk-17-jdk-headless
fi

mkdir -p /opt/advocacy /etc/advocacy /github/prod
chown -R advocacy:advocacy /opt/advocacy /github/prod

if [ ! -d "${JAVA21_DIR}" ]; then
  TMP_DIR="$(mktemp -d)"
  curl -fsSL -o "${TMP_DIR}/jdk21.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
  mkdir -p "${JAVA21_DIR}"
  tar -xzf "${TMP_DIR}/jdk21.tar.gz" --strip-components=1 -C "${JAVA21_DIR}"
  rm -rf "${TMP_DIR}"
fi

if [ ! -x "${MAVEN_DIR}/bin/mvn" ]; then
  TMP_DIR="$(mktemp -d)"
  curl -fsSL -o "${TMP_DIR}/maven.tar.gz" \
    "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  mkdir -p "${MAVEN_DIR}"
  tar -xzf "${TMP_DIR}/maven.tar.gz" --strip-components=1 -C "${MAVEN_DIR}"
  rm -rf "${TMP_DIR}"
fi

install -m 644 \
  "/github/prod/user-management/deploy/prod/advocacy-user-management-prod.service.example" \
  "/etc/systemd/system/advocacy-user-management-prod.service"
install -m 644 \
  "/github/prod/publication-service/deploy/prod/advocacy-publication-service-prod.service.example" \
  "/etc/systemd/system/advocacy-publication-service-prod.service"
install -m 644 \
  "/github/prod/notification-service/deploy/prod/advocacy-notification-service-prod.service.example" \
  "/etc/systemd/system/advocacy-notification-service-prod.service"
install -m 644 \
  "/github/prod/media-service/deploy/prod/advocacy-media-service-prod.service.example" \
  "/etc/systemd/system/advocacy-media-service-prod.service"

systemctl daemon-reload
systemctl enable advocacy-user-management-prod.service
systemctl enable advocacy-publication-service-prod.service
systemctl enable advocacy-notification-service-prod.service
systemctl enable advocacy-media-service-prod.service

mkdir -p /opt/advocacy/user-management-prod/current
mkdir -p /opt/advocacy/publication-service-prod/current
mkdir -p /opt/advocacy/notification-service-prod/current
mkdir -p /opt/advocacy/media-service-prod/current

chown -R advocacy:advocacy /opt/advocacy /github/prod /etc/advocacy

echo "Production backend host bootstrap completed"
