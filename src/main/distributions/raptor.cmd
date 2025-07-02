@echo off
setlocal

set "BASE_DIR=%~dp0"

rem Check if there is a JRE in the "java" directory
if exist "%BASE_DIR%\java\bin\java.exe" (
    set "JAVA_EXEC=%BASE_DIR%\java\bin\java.exe"
) else (
    rem Check if java is in the PATH
    where java >nul 2>nul
    if errorlevel 1 (
        echo ERROR: Java Runtime Environment not found. Please install Java or place it in the 'java' directory.
        exit /b 1
    ) else (
        set "JAVA_EXEC=java"
    )
)

rem Run the Java application using the current directory as the classpath
"%JAVA_EXEC%" -cp "%BASE_DIR%*;%BASE_DIR%libs\*" com.github.trosenkrantz.raptor.Main %*

endlocal
