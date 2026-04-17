#!/bin/bash
set -euo pipefail

echo "timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "hostname=$(hostname)"
echo

echo "[service]"
systemctl is-active mysql
echo

echo "[memory]"
free -h
echo

echo "[mysql_variables]"
mysql -Nse "SHOW VARIABLES WHERE Variable_name IN ('innodb_buffer_pool_size','max_connections','tmp_table_size','max_heap_table_size','slow_query_log','long_query_time','slow_query_log_file');"
echo

echo "[mysql_status]"
mysql -Nse "SHOW STATUS WHERE Variable_name IN ('Threads_connected','Threads_running','Max_used_connections','Slow_queries','Queries','Questions','Uptime');"
echo

echo "[mysql_processlist]"
mysql -Nse "SHOW FULL PROCESSLIST" | head -n 20 || true
echo

echo "[slow_log]"
if [ -f /var/log/mysql/socialripple-slow.log ]; then
  ls -lh /var/log/mysql/socialripple-slow.log
  tail -n 20 /var/log/mysql/socialripple-slow.log || true
else
  echo "slow log not present"
fi
