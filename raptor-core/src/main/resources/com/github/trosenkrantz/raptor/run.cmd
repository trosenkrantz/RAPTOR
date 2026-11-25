@echo off
setlocal

set "CONFIG_DIR=%~dp0"
if not exist "%CONFIG_DIR%config.json" (
    echo ERROR: Configuration file not found: %CONFIG_DIR%config.json
    exit /b 1
)
set "RAPTOR_ANCHOR_DIR=%CONFIG_DIR%"
call "%CONFIG_DIR%\..\..\raptor.cmd"
