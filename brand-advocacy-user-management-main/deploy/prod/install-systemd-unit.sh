#!/bin/bash
set -euo pipefail

UNIT_NAME="advocacy-user-management-prod.service"
SOURCE_FILE="$(cd "$(dirname "$0")" && pwd)/${UNIT_NAME}.example"
TARGET_FILE="/etc/systemd/system/${UNIT_NAME}"

cp "${SOURCE_FILE}" "${TARGET_FILE}"
systemctl daemon-reload
systemctl enable "${UNIT_NAME}"
echo "Installed ${UNIT_NAME}"
