#!/bin/bash
set -euo pipefail

JAVA21_DIR="/opt/jdk-21"
MAVEN_DIR="/opt/apache-maven"
MAVEN_VERSION="3.9.10"

if ! id advocacy >/dev/null 2>&1; then
  useradd --system --create-home --shell /usr/sbin/nologin advocacy
fi

mkdir -p /opt/advocacy /etc/advocacy /github/dev2
chown -R advocacy:advocacy /opt/advocacy /github/dev2

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
  "/github/dev2/user-management/deploy/dev2/advocacy-user-management-dev2.service.example" \
  "/etc/systemd/system/advocacy-user-management-dev2.service"
install -m 644 \
  "/github/dev2/publication-service/deploy/dev2/advocacy-publication-service-dev2.service.example" \
  "/etc/systemd/system/advocacy-publication-service-dev2.service"
install -m 644 \
  "/github/dev2/notification-service/deploy/dev2/advocacy-notification-service-dev2.service.example" \
  "/etc/systemd/system/advocacy-notification-service-dev2.service"
install -m 644 \
  "/github/dev2/media-service/deploy/dev2/advocacy-media-service-dev2.service.example" \
  "/etc/systemd/system/advocacy-media-service-dev2.service"

systemctl daemon-reload
systemctl enable advocacy-user-management-dev2.service
systemctl enable advocacy-publication-service-dev2.service
systemctl enable advocacy-notification-service-dev2.service
systemctl enable advocacy-media-service-dev2.service

mkdir -p /opt/advocacy/user-management-dev2/current
mkdir -p /opt/advocacy/publication-service-dev2/current
mkdir -p /opt/advocacy/notification-service-dev2/current
mkdir -p /opt/advocacy/media-service-dev2/current

chown -R advocacy:advocacy /opt/advocacy /github/dev2 /etc/advocacy

echo "Dev2 backend host bootstrap completed"
