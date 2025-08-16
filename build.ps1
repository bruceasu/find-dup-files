# Powershell.exe  -ExecutionPolicy Bypass -File build.ps1

# Build script with JDK 25
$env:JAVA_HOME = "C:\green\jdk-25.0.1+8"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "Building with JDK 25..." -ForegroundColor Cyan
.\mvnw.cmd clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "Build Successful!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green

    # Check for Fat JAR
    $fatJar = "target\find-dup-files-fat.jar"
    $normalJar = "target\find-dup-files-1.0.0-SNAPSHOT.jar"

    Write-Host "`nGenerated JARs: $fatJar, $normalJar" -ForegroundColor Yellow

    if (Test-Path $normalJar) {
        $normalSize = (Get-Item $normalJar).Length / 1MB
        Write-Host "  Normal JAR: $normalJar" -ForegroundColor White
        Write-Host "              Size: $([math]::Round($normalSize, 2)) MB" -ForegroundColor Gray
    }

    if (Test-Path $fatJar) {
        $fatSize = (Get-Item $fatJar).Length / 1MB
        Write-Host "  Fat JAR:    $fatJar" -ForegroundColor White
        Write-Host "              Size: $([math]::Round($fatSize, 2)) MB (includes all dependencies)" -ForegroundColor Gray
        Write-Host "`n[OK] Fat JAR is ready! This JAR contains all dependencies." -ForegroundColor Green
    }

}
else {
    Write-Host "`nBuild Failed!" -ForegroundColor Red
    exit 1
}