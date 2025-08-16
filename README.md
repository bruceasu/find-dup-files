# Find Dup Files (fdf)

A CLI tool to index files, detect duplicate candidates, verify full hashes, and generate cleanup scripts using a local SQLite database.

## What It Does

- Scans directories and stores file metadata plus quick hashes in SQLite
- Groups duplicate candidates into `group_id` values
- Verifies full SHA-256 hashes for selected groups
- Exports report CSV files
- Generates commented delete scripts (`.sh` and `.ps1`)
- Maintains and prunes the database

## Build

```bash
mvn clean package
```

Main class: `me.asu.fdf.Dedup`

Typical runnable artifact:

- `target/find-dup-files-1.0.0-SNAPSHOT-fat.jar`

## Run

### Direct Java

```bash
java -jar target/find-dup-files-1.0.0-SNAPSHOT-fat.jar <command> [options]
```

### Launcher Scripts

- Windows: `src/main/scripts/fdf.bat`
- Linux/macOS: `src/main/scripts/fdf.sh`

Both launchers expect `find-dup-files.jar` in the same directory as the script.

## Commands

### `scan`

Scan one or more roots and upsert fingerprints into DB.

```bash
java -jar ... scan --db <db> <root1> [root2 ...] [--from-disk <name>] [--workers <N>]
```

Options:

- `--db`, `-d`: SQLite DB file (required)
- `--from-disk`, `-f`: source label stored in DB (default: `local`)
- `--workers`, `-w`: worker count (default: `3`)

### `report`

Generate duplicate report CSV from DB.

```bash
java -jar ... report --db <db> [--out <file>] [--mode quick|full]
```

Options:

- `--db`, `-d`: SQLite DB file (required)
- `--out`, `-o`: output CSV (default: `report.csv`)
- `--mode`, `-m`: `quick` (group by `size + quick_hash`) or `full` (group by `full_hash`)

### `genscript`

Generate cleanup helper files from grouped records.

```bash
java -jar ... genscript --db <db> [--out-prefix <prefix>]
```

Outputs:

- `<prefix>.csv`
- `<prefix>.sh`
- `<prefix>.ps1`

Options:

- `--db`, `-d`: SQLite DB file (required)
- `--out-prefix`, `-p`: output prefix (default: `delete_dup`)

### `verify`

Compute and store full SHA-256 hashes for one duplicate group and one source disk.

```bash
java -jar ... verify --db <db> --group-id <id> --from-disk <name> [--workers <N>]
```

Options:

- `--db`, `-d`: SQLite DB file (required)
- `--group-id`, `-g`: group id to verify (required)
- `--from-disk`, `-f`: source label filter (required)
- `--workers`, `-w`: worker count (default: `1`)

### `maintain`

Run SQLite maintenance operations.

```bash
java -jar ... maintain --db <db> --mode init|incremental|vacuum-into [--pages <N>] [--out <file>]
```

Options:

- `--db`, `-d`: SQLite DB file (required)
- `--mode`, `-m`: maintenance mode (required)
- `--pages`, `-p`: page count for incremental vacuum (default: `100`)
- `--out`, `-o`: output DB for `vacuum-into` mode

Modes:

- `init`: set WAL + `auto_vacuum=INCREMENTAL`, then `VACUUM`
- `incremental`: run `PRAGMA incremental_vacuum(<pages>)`
- `vacuum-into`: compact into a new DB file

### `prune`

Delete DB records for files that no longer exist on disk.

```bash
java -jar ... prune <db> --from-disk <name> --prefix <path-prefix|/> [--dry-run]
```

Options:

- positional `<db>`: SQLite DB file (required)
- `--from-disk`: source label filter (default: `local`)
- `--prefix`: path prefix filter (required, use `/` for all)
- `--dry-run`: report only, do not delete

## Typical Workflow

1. Scan files into DB.
2. Generate quick report.
3. Verify full hashes for candidate groups.
4. Generate delete scripts.
5. Prune missing entries and run maintenance.

Example:

```bash
fdf scan --db cache.db D:\projects --from-disk local --workers 4
fdf report --db cache.db --out report.csv --mode quick
fdf verify --db cache.db --group-id 1 --from-disk local --workers 2
fdf report --db cache.db --out report_full.csv --mode full
fdf genscript --db cache.db --out-prefix delete_dup
fdf prune cache.db --from-disk local --prefix / --dry-run
fdf maintain --db cache.db --mode incremental --pages 200
```

## License

Apache License 2.0.
