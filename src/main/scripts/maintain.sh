#!/usr/bin/env bash
set -euo pipefail

DEFAULT_DB="cache.db"

cmd="${1:-}"
arg2="${2:-}"
arg3="${3:-}"

usage() {
  cat <<EOF
Usage:
  $0 init [db]
  $0 incremental [db] [pages]
  $0 vacuum-into [db] <new_db>
  $0 check [db]
  $0 info [db]

Default DB:
  If [db] is omitted, use ./${DEFAULT_DB}
EOF
  exit 1
}

[[ -z "$cmd" ]] && usage

# -------- argument normalization --------
db="$DEFAULT_DB"
pages=100
newdb=""

case "$cmd" in
  init|check|info)
    [[ -n "$arg2" ]] && db="$arg2"
    ;;

  incremental)
    if [[ -n "$arg2" && "$arg2" =~ ^[0-9]+$ ]]; then
      pages="$arg2"
    else
      [[ -n "$arg2" ]] && db="$arg2"
      [[ -n "$arg3" ]] && pages="$arg3"
    fi
    ;;

  vacuum-into)
    if [[ -z "$arg2" ]]; then usage; fi
    if [[ -n "$arg3" ]]; then
      db="$arg2"
      newdb="$arg3"
    else
      newdb="$arg2"
    fi
    ;;

  *)
    usage
    ;;
esac

# -------- existence check --------
if [[ "$cmd" != "vacuum-into" && ! -f "$db" ]]; then
  echo "DB not found: $db"
  exit 1
fi

# -------- commands --------
case "$cmd" in
  init)
    echo "[init] db=$db"
    sqlite3 "$db" <<EOF
PRAGMA journal_mode=WAL;
PRAGMA auto_vacuum=INCREMENTAL;
VACUUM;
EOF
    echo "[init] done"
    ;;

  incremental)
    echo "[incremental] db=$db pages=$pages"
    sqlite3 "$db" "PRAGMA incremental_vacuum($pages);"
    echo "[incremental] done"
    ;;

  vacuum-into)
    echo "[vacuum-into] $db -> $newdb"
    sqlite3 "$db" "VACUUM INTO '$newdb';"
    echo "[vacuum-into] integrity check"
    sqlite3 "$newdb" "PRAGMA integrity_check;"
    echo "[vacuum-into] done"
    ;;

  check)
    echo "[check] db=$db"
    sqlite3 "$db" "PRAGMA integrity_check;"
    ;;

  info)
    echo "[info] db=$db"
    sqlite3 "$db" <<EOF
.headers on
.mode column
SELECT
  page_size,
  page_count,
  freelist_count,
  page_count * page_size AS approx_bytes
FROM pragma_page_count(), pragma_page_size(), pragma_freelist_count();
EOF
    ls -lh "$db"
    ;;
esac
