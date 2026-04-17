#!/bin/bash
set -euo pipefail

SLOW_LOG_FILE="${SLOW_LOG_FILE:-/var/log/mysql/socialripple-slow.log}"
LONG_QUERY_TIME="${LONG_QUERY_TIME:-1}"
MYSQL_EXTRA_CONF="/etc/mysql/conf.d/socialripple-slow-query.cnf"

install -d -m 755 /var/log/mysql
touch "${SLOW_LOG_FILE}"
chown mysql:mysql "${SLOW_LOG_FILE}"
chmod 640 "${SLOW_LOG_FILE}"

cat > "${MYSQL_EXTRA_CONF}" <<EOF
[mysqld]
slow_query_log=ON
slow_query_log_file=${SLOW_LOG_FILE}
long_query_time=${LONG_QUERY_TIME}
log_queries_not_using_indexes=OFF
EOF

mysql -e "SET GLOBAL slow_query_log = 'ON';"
mysql -e "SET GLOBAL long_query_time = ${LONG_QUERY_TIME};"
mysql -e "SET GLOBAL slow_query_log_file = '${SLOW_LOG_FILE}';"

systemctl restart mysql
systemctl is-active --quiet mysql

echo "Enabled slow query log at ${SLOW_LOG_FILE} with long_query_time=${LONG_QUERY_TIME}"
