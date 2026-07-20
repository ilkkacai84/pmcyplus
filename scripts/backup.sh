#!/usr/bin/env bash
set -euo pipefail

backup_dir="${1:-./backups}"
timestamp="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup_dir"

database_file="$backup_dir/project-management-$timestamp.sql.gz"
files_file="$backup_dir/project-files-$timestamp.tar.gz"

docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers "$MYSQL_DATABASE"' \
  | gzip > "$database_file"
docker compose exec -T backend tar -czf - -C /app/data files > "$files_file"

echo "Database backup: $database_file"
echo "File backup: $files_file"
