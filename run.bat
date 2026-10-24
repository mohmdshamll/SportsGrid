@echo off
REM ==============================================================================
REM SportGrid Windows Launcher (Direct Java Execution)
REM Requires Java 21 or higher installed on your Windows PC.
REM ==============================================================================

setlocal enabledelayedexpansion

echo === Starting SportGrid Desktop Application ===

where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: 'java' command not found.
    echo Please install Java 21 or higher (https://adoptium.net/temurin/releases/?version=21) and try again.
    pause
    exit /b 1
)

set "CP=target\sportgrid-1.0.0.jar;lib\mysql-connector-j-8.4.0.jar;lib\*"
if not exist "target\sportgrid-1.0.0.jar" (
    echo Building SportGrid desktop JAR with Maven...
    call mvnw.cmd package -DskipTests
    if !ERRORLEVEL! neq 0 (
        echo.
        echo ERROR: Maven build failed.
        pause
        exit /b 1
    )
)

if not exist "target\sportgrid-1.0.0.jar" (
    echo.
    echo ERROR: Expected desktop jar 'target\sportgrid-1.0.0.jar' was not found.
    pause
    exit /b 1
)

if not exist "db.properties" (
    if not exist "%USERPROFILE%\.sportgrid\db.properties" (
        echo.
        echo [INFO] db.properties not found in current folder or %USERPROFILE%\.sportgrid\
        echo [INFO] Using defaults (root, empty password).
        echo [INFO] If connection fails, copy db.properties.example to db.properties and set your MySQL password.
        echo.
    )
)

echo Launching SportGrid GUI...
java -cp "%CP%" main.Main

if %ERRORLEVEL% neq 0 (
    echo.
    echo Application exited with code %ERRORLEVEL%.
    pause
)
