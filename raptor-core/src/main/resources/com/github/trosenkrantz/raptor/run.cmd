@echo off
setlocal

set "CONFIG_DIR=%~dp0"
call "%CONFIG_DIR%\..\..\raptor.cmd" --config "%CONFIG_DIR%config.json"
