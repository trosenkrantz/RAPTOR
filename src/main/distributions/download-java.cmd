@echo off
setlocal

set "BASE_DIR=%~dp0"
set "JAVA_DIR=%BASE_DIR%java"
set "ZULU_VERSION=21.0.10"
set "ZULU_BUILD=21.48.17-ca"
set "ZULU_BASE_URL=https://cdn.azul.com/zulu/bin"
set "EXPECTED_SHA256=82b99c1cc9d0a8b4d6307a8ee38ae156acc63931b20263f159ec1909307e7ce9"

if exist "%JAVA_DIR%" (
    echo Java already present.
    exit /b 0
)

set "BASE_FILENAME=zulu%ZULU_BUILD%-jre%ZULU_VERSION%-win_x64"
set "FILENAME=%BASE_FILENAME%.zip"
set "URL=%ZULU_BASE_URL%/%FILENAME%"
set "TMP_FILE=%BASE_DIR%%FILENAME%"

echo Downloading %FILENAME%.
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%URL%' -OutFile '%TMP_FILE%'"

for /f "tokens=*" %%a in ('powershell -Command "(Get-FileHash '%TMP_FILE%' -Algorithm SHA256).Hash.ToLower()"') do set "ACTUAL_SHA256=%%a"

if not "%ACTUAL_SHA256%"=="%EXPECTED_SHA256%" (
    echo Checksum verification failed.
    exit /b 1
)
echo Verified checksum.

powershell -Command "Expand-Archive -Path '%TMP_FILE%' -DestinationPath '%BASE_DIR%' -Force"

rem Rename the newly created dir
move "%BASE_DIR%%BASE_FILENAME%" "%JAVA_DIR%" >nul

del "%TMP_FILE%"

echo Java set up.

endlocal
