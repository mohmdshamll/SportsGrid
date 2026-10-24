@echo off
REM ==============================================================================
REM SportGrid Windows Packaging Script
REM Builds the Java Swing desktop application and packages it into a native
REM Windows application folder with SportGrid.exe using jpackage.
REM MUST BE RUN ON A WINDOWS MACHINE WITH JDK 21+ (JDK 21 LTS recommended).
REM ==============================================================================

setlocal enabledelayedexpansion

echo === SportGrid Windows Application Packager ===

REM 1. Check for jpackage
where jpackage >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\jpackage.exe" (
            set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
        )
    )
) else (
    set "JPACKAGE=jpackage"
)

if not defined JPACKAGE (
    echo.
    echo ERROR: 'jpackage' tool not found.
    echo Please install JDK 21 or higher (https://adoptium.net/temurin/releases/?version=21) and ensure JAVA_HOME is set.
    pause
    exit /b 1
)

echo Using jpackage: %JPACKAGE%

REM 2. Check or build desktop JAR using Maven Wrapper
set "DESKTOP_JAR=target\sportgrid-1.0.0.jar"
if not exist "%DESKTOP_JAR%" (
    echo Building SportGrid desktop artifacts with Maven...
    call mvnw.cmd package -DskipTests
    if !ERRORLEVEL! neq 0 (
        echo.
        echo ERROR: Maven build failed.
        pause
        exit /b 1
    )
) else (
    echo Using desktop JAR: %DESKTOP_JAR%
)

if not exist "%DESKTOP_JAR%" (
    echo.
    echo ERROR: Expected desktop jar '%DESKTOP_JAR%' was not found.
    pause
    exit /b 1
)

REM 3. Select MySQL JDBC driver
set "MYSQL_DRIVER=lib\mysql-connector-j-8.4.0.jar"
if not exist "%MYSQL_DRIVER%" (
    set "MYSQL_DRIVER=lib\mysql-connector-j-26.10.0.jar"
)
if not exist "%MYSQL_DRIVER%" (
    echo.
    echo ERROR: MySQL JDBC connector jar not found in lib\.
    pause
    exit /b 1
)
echo Using MySQL Driver: %MYSQL_DRIVER%

REM 4. Prepare jpackage input staging directory
set "INPUT_DIR=target\package-input"
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
copy "%DESKTOP_JAR%" "%INPUT_DIR%\" >nul
copy "%MYSQL_DRIVER%" "%INPUT_DIR%\" >nul

REM 5. Build Windows Application Image (.exe + bundled runtime)
set "OUTPUT_DIR=dist"
if exist "%OUTPUT_DIR%\SportGrid" rmdir /s /q "%OUTPUT_DIR%\SportGrid"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo Invoking jpackage to create Windows application bundle...
"%JPACKAGE%" ^
    --type app-image ^
    --name "SportGrid" ^
    --app-version "1.0.0" ^
    --input "%INPUT_DIR%" ^
    --main-jar "sportgrid-1.0.0.jar" ^
    --main-class "main.Main" ^
    --dest "%OUTPUT_DIR%" ^
    --add-modules "java.base,java.desktop,java.sql,java.naming,java.management,java.security.sasl,java.prefs,java.logging,java.xml"

if exist "%OUTPUT_DIR%\SportGrid\SportGrid.exe" (
    echo.
    echo =================================================================
    echo SUCCESS: Windows application bundle created successfully!
    echo Location: %OUTPUT_DIR%\SportGrid\SportGrid.exe
    echo You can distribute the '%OUTPUT_DIR%\SportGrid' folder to any Windows user.
    echo =================================================================
    pause
) else (
    echo.
    echo ERROR: Failed to generate %OUTPUT_DIR%\SportGrid\SportGrid.exe.
    pause
    exit /b 1
)
