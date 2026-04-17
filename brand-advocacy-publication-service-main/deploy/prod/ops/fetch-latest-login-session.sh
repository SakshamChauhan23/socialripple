#!/bin/bash
set -euo pipefail

DB_NAME="${DB_NAME:-advocacy_db}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
PYTHON_BIN="${PYTHON_BIN:-python3}"

if ! command -v "${MYSQL_BIN}" >/dev/null 2>&1; then
  echo "mysql client is required" >&2
  exit 1
fi

if ! command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
  echo "python3 is required" >&2
  exit 1
fi

session_query=$(
  cat <<'SQL'
SELECT
  u.email,
  u.organization_id,
  ls.jwt_token,
  COALESCE(DATE_FORMAT(ls.expires_at, '%Y-%m-%dT%H:%i:%sZ'), '')
FROM login_sessions ls
JOIN users u ON u.id = ls.user_id
WHERE ls.jwt_token IS NOT NULL
  AND ls.jwt_token <> ''
  AND (ls.expires_at IS NULL OR ls.expires_at > UTC_TIMESTAMP())
ORDER BY ls.id DESC
LIMIT 1;
SQL
)

session_row="$("${MYSQL_BIN}" -N -B -u"${MYSQL_USER}" -D "${DB_NAME}" -e "${session_query}")"

if [ -n "${session_row}" ]; then
  printf '%s\n' "${session_row}"
  exit 0
fi

fallback_row="$("${MYSQL_BIN}" -N -B -u"${MYSQL_USER}" -D "${DB_NAME}" -e "
SELECT
  u.email,
  u.organization_id,
  jwt.parameter_value,
  exp.parameter_value
FROM users u
JOIN cnfg_config_parameters jwt ON jwt.parameter_name = 'JWT_SECRET'
JOIN cnfg_config_parameters exp ON exp.parameter_name = 'JWT_EXPIRATION_MS'
WHERE u.status = 'ACTIVE'
  AND u.organization_id IS NOT NULL
ORDER BY u.id ASC
LIMIT 1;
")"

if [ -z "${fallback_row}" ]; then
  exit 0
fi

IFS=$'\t' read -r perf_user perf_tenant jwt_secret jwt_expiration_ms <<< "${fallback_row}"

if [ -z "${perf_user}" ] || [ -z "${perf_tenant}" ] || [ -z "${jwt_secret}" ] || [ -z "${jwt_expiration_ms}" ]; then
  exit 0
fi

generated_file="$(mktemp)"
"${PYTHON_BIN}" - "${perf_user}" "${jwt_secret}" "${jwt_expiration_ms}" > "${generated_file}" <<'PY'
import base64
import datetime as dt
import hashlib
import hmac
import json
import re
import sys

user = sys.argv[1]
secret = sys.argv[2]
expiry_ms = int(sys.argv[3])
now = dt.datetime.now(dt.timezone.utc)
exp = now + dt.timedelta(milliseconds=expiry_ms)

def b64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")

sanitized_secret = re.sub(r"[^A-Za-z0-9+/=]", "", secret)
sanitized_secret = sanitized_secret[: len(sanitized_secret) - (len(sanitized_secret) % 4)]
secret_bytes = base64.b64decode(sanitized_secret) if sanitized_secret else b""

header = {"alg": "HS512"}
payload = {
    "sub": user,
    "iat": int(now.timestamp()),
    "exp": int(exp.timestamp()),
}

header_b64 = b64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
payload_b64 = b64url(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
signing_input = f"{header_b64}.{payload_b64}".encode("ascii")
signature = hmac.new(secret_bytes, signing_input, hashlib.sha512).digest()
token = f"{header_b64}.{payload_b64}.{b64url(signature)}"
print(token)
print(exp.strftime("%Y-%m-%dT%H:%M:%SZ"))
PY

generated_token="$(sed -n '1p' "${generated_file}")"
generated_expiry="$(sed -n '2p' "${generated_file}")"
rm -f "${generated_file}"

if [ -z "${generated_token}" ]; then
  exit 0
fi

printf '%s\t%s\t%s\t%s\n' "${perf_user}" "${perf_tenant}" "${generated_token}" "${generated_expiry}"
