@echo off
call :getScriptLocation APP_DIR
java  -jar %APP_DIR%\find-dup-files.jar %*

:: If this script is in `%PATH%` and called quoted without a full path (e.g., `"js"`), `%~dp0` is expanded to `cwd`
:: rather than the path to the script.
:: This does not happen if `%~dp0` is accessed in a subroutine.
:getScriptLocation variableName
  set "%~1=%~dp0"
  exit /b 0