# Powershell.exe  -ExecutionPolicy Bypass -File create-distribution.ps1

# Create distribution package for SQL2POJO
# This script creates a ready-to-distribute package with JAR and scripts

param(
  [string]$Version = "1.0.0-SNAPSHOT"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Find duplicated files Distribution Package Creator" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$distDir = "distribution"
$fatJar = "target\find-dup-files-$Version-fat.jar"

# 1. Check if Fat JAR exists
if (-not (Test-Path $fatJar)) {
  Write-Host "Error: Fat JAR not found: $fatJar" -ForegroundColor Red
  Write-Host "Please run: .\build.ps1" -ForegroundColor Yellow
  exit 1
}

# 2. Clean and create distribution directory
Write-Host "[1/5] Preparing distribution directory..." -ForegroundColor Yellow
if (Test-Path $distDir) {
  Remove-Item $distDir -Recurse -Force
}
New-Item -ItemType Directory -Path $distDir | Out-Null
Write-Host "      [OK] Directory created" -ForegroundColor Green

# 3. Copy Fat JAR and rename
Write-Host "[2/5] Copying Fat JAR..." -ForegroundColor Yellow
Copy-Item $fatJar "$distDir\fdf.jar"
$jarSize = (Get-Item "$distDir\fdf.jar").Length / 1MB
Write-Host "      [OK] fdf.jar ($([math]::Round($jarSize, 2)) MB)" -ForegroundColor Green

# 4. Copy execution scripts
Write-Host "[3/5] Copying execution scripts..." -ForegroundColor Yellow
Copy-Item "src\main\scripts\fdf.bat" $distDir\
Copy-Item "src\main\scripts\fdf.sh" $distDir\
Copy-Item "src\main\scripts\maintain.sh" $distDir\
Write-Host "      [OK] fdf.bat" -ForegroundColor Green
Write-Host "      [OK] fdf.sh" -ForegroundColor Green
Write-Host "      [OK] maintain.sh" -ForegroundColor Green

# 5. Create README
Write-Host "[4/5] Creating README..." -ForegroundColor Yellow
$readme = @"
# Find duplicated files $Version - Distribution Package

Find duplicated files is a tool to scan files, identify duplicate files 
based on their content hashes, and manage them effectively.

## Requirements

- JDK 25 or later
- SQLite3 (for database operations)

## Quick Start

### Windows Command Prompt
``````cmd
fdf.cmd -p com.example
``````

### Linux/Mac
``````bash
chmod +x fdf.sh
./fdf.sh -p com.example
``````

## Usage

### Basic Syntax
java -jar fdf.jar <command> [options]
Commands:
    scan         Scan files and collect fingerprints into DB
    report       Generate duplicate report from DB
    genscript    Generate delete scripts from DB
    verify       Verify file hashes in DB
    maintain     Maintain the database (SQLite PRAGMA operations)
    prune        Prune missing files from DB
options:
    --db, -d <db>               SQLite database file
    --from-disk, -f <path>      Base path on disk for scanning or verifying
    --workers, -w <N>           Number of worker threads (default: 3)
    --out, -o <file>            Output file
    --group-id, -g <N>          Group ID for verify command
    --out-prefix, -p <prefix>   Output file prefix for delete scripts
specific to maintain command:
    --mode, -m <mode>           Mode for maintain command: [init|incremental|vacuum-into]
    --pages, -p <N>             Number of pages for vacuum-into (default: 100)
specific to prune command:
    --prefix <path>             Prefix path to prune (use '/' for all)
    --dry-run                   Dry run mode, do not actually delete
Examples:
    java -jar fdf.jar scan --db <db> <root...> [--from-disk X] [--workers N]
    java -jar fdf.jar report --db <db> [--out report.csv]   (default: report.csv)
    java -jar fdf.jar genscript --db <db> [--out-prefix delete_dup]   (default: delete_dup)
    java -jar fdf.jar verify --db <db> --group-id N --from-disk X [--workers N]
    java -jar fdf.jar maintain --db <db> --mode [init|incremental|vacuum-into] [--out file] [--pages N]

## Support

For issues and documentation, visit:
https://github.com/bruceasu/find-dup-files

## License

Apache License 2.0

---
Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
Version: $Version
"@

Set-Content -Path "$distDir\README.txt" -Value $readme -Encoding UTF8
Write-Host "      [OK] README.txt created" -ForegroundColor Green

# 6. Create zip archive
Write-Host "[5/5] Creating archive..." -ForegroundColor Yellow
$zipFile = "fdf-$Version-dist.zip"
if (Test-Path $zipFile) {
  Remove-Item $zipFile -Force
}
Compress-Archive -Path "$distDir\*" -DestinationPath $zipFile
$zipSize = (Get-Item $zipFile).Length / 1MB
Write-Host "      [OK] $zipFile ($([math]::Round($zipSize, 2)) MB)" -ForegroundColor Green

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Distribution Package Created!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Package: " -NoNewline; Write-Host $zipFile -ForegroundColor Yellow
Write-Host "Size:    " -NoNewline; Write-Host "$([math]::Round($zipSize, 2)) MB" -ForegroundColor Yellow
Write-Host ""
Write-Host "Contents:" -ForegroundColor White
Write-Host "  - fsf.jar          (Fat JAR with all dependencies)" -ForegroundColor Gray
Write-Host "  - fsf.cmd          (Windows CMD script)" -ForegroundColor Gray
Write-Host "  - fsf.sh           (Unix/Linux/Mac script)" -ForegroundColor Gray
Write-Host "  - README.txt       (Usage instructions)" -ForegroundColor Gray
Write-Host ""
Write-Host "To test:" -ForegroundColor Yellow
Write-Host "  1. Expand-Archive $zipFile -DestinationPath test-dist" -ForegroundColor White
Write-Host "  2. cd test-dist" -ForegroundColor White
Write-Host "  3. .\fsf.bat -h" -ForegroundColor White
Write-Host ""
