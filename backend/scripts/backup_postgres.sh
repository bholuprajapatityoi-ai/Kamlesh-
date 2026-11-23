#!/usr/bin/env bash
set -euo pipefail

# Usage: DATABASE_URL=postgresql://user:pass@host:5432/golu_db ./scripts/backup_postgres.sh [output_dir]
OUT_DIR="${1:-./backups}"
mkdir -p "$OUT_DIR"
TIMESTAMP="$(date -u +%Y%m%d_%H%M%S)"
OUT_FILE="$OUT_DIR/golu_backup_${TIMESTAMP}.dump"

if [[ -z "${DATABASE_URL:-}" ]]; then
  echo "ERROR: Set DATABASE_URL before running backup."
  exit 1
fi

pg_dump --format=custom --no-owner --dbname="$DATABASE_URL" --file="$OUT_FILE"
echo "Backup created: $OUT_FILE"
