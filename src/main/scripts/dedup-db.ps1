# Powershell.exe  -ExecutionPolicy Bypass -File dedup-db.ps1

param(
    [Parameter(Mandatory = $true)][string]$Command,
    [string]$Db = "cache.db",
    [int]$Pages = 100,
    [string]$NewDb
)

function Usage {
    Write-Host @"
Usage:
  dedup-db.ps1 init [db]
  dedup-db.ps1 incremental [db] [pages]
  dedup-db.ps1 vacuum-into [db] <new_db>
  dedup-db.ps1 check [db]
  dedup-db.ps1 info [db]

Default DB:
  If db is omitted, use .\cache.db
"@
    exit 1
}

if ($Command -ne "vacuum-into" -and !(Test-Path $Db)) {
    Write-Error "DB not found: $Db"
    exit 1
}

switch ($Command) {

    "init" {
        Write-Host "[init] db=$Db"
        sqlite3 $Db @"
PRAGMA journal_mode=WAL;
PRAGMA auto_vacuum=INCREMENTAL;
VACUUM;
"@
        Write-Host "[init] done"
    }

    "incremental" {
        Write-Host "[incremental] db=$Db pages=$Pages"
        sqlite3 $Db "PRAGMA incremental_vacuum($Pages);"
        Write-Host "[incremental] done"
    }

    "vacuum-into" {
        if (-not $NewDb) { Usage }
        Write-Host "[vacuum-into] $Db -> $NewDb"
        sqlite3 $Db "VACUUM INTO '$NewDb';"
        Write-Host "[vacuum-into] integrity check"
        sqlite3 $NewDb "PRAGMA integrity_check;"
        Write-Host "[vacuum-into] done"
    }

    "check" {
        Write-Host "[check] db=$Db"
        sqlite3 $Db "PRAGMA integrity_check;"
    }

    "info" {
        Write-Host "[info] db=$Db"
        sqlite3 $Db @"
.headers on
.mode column
SELECT
  page_size,
  page_count,
  freelist_count,
  page_count * page_size AS approx_bytes
FROM pragma_page_count(), pragma_page_size(), pragma_freelist_count();
"@
        Get-Item $Db | Format-List Name, Length, LastWriteTime
    }

    default {
        Usage
    }
}
