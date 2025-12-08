@echo off
REM YAFVA.JAR Windows Service Uninstallation Script using NSSM
REM This script uninstalls the YAFVA.JAR Windows service

setlocal enabledelayedexpansion

REM Check for administrator privileges
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: This script requires administrator privileges.
    echo Please right-click and select "Run as administrator"
    pause
    exit /b 1
)

REM Get the directory where this script is located
set "INSTALL_DIR=%~dp0"
REM Remove trailing backslash
set "INSTALL_DIR=%INSTALL_DIR:~0,-1%"

REM Define paths
set "NSSM_EXE=%INSTALL_DIR%\bin\nssm.exe"

REM Service configuration
set "SERVICE_NAME=yafvajar"

echo ========================================
echo YAFVA.JAR Service Uninstallation
echo ========================================
echo.
echo Service name: %SERVICE_NAME%
echo.

REM Verify NSSM exists
if not exist "%NSSM_EXE%" (
    echo ERROR: NSSM not found at: %NSSM_EXE%
    echo.
    echo Please download NSSM from https://nssm.cc/download
    echo and place nssm.exe in the bin\ folder.
    echo See bin\README.md for detailed instructions.
    pause
    exit /b 1
)

REM Check if service exists
sc query "%SERVICE_NAME%" >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: Service '%SERVICE_NAME%' does not exist.
    echo Nothing to uninstall.
    pause
    exit /b 0
)

REM Check if service is running
sc query "%SERVICE_NAME%" | find "RUNNING" >nul 2>&1
if %errorlevel% equ 0 (
    echo Service is currently running. Stopping...
    "%NSSM_EXE%" stop "%SERVICE_NAME%" >nul 2>&1

    REM Wait for service to stop (max 30 seconds)
    set /a timeout=30
    :wait_loop
    sc query "%SERVICE_NAME%" | find "STOPPED" >nul 2>&1
    if %errorlevel% equ 0 goto stopped
    set /a timeout-=1
    if %timeout% leq 0 (
        echo WARNING: Service did not stop within 30 seconds
        echo Proceeding with uninstallation anyway...
        goto uninstall
    )
    timeout /t 1 /nobreak >nul
    goto wait_loop

    :stopped
    echo Service stopped successfully.
)

:uninstall
echo Uninstalling service...
echo.

REM Remove the service
"%NSSM_EXE%" remove "%SERVICE_NAME%" confirm >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Failed to uninstall service
    pause
    exit /b 1
)

echo SUCCESS: Service '%SERVICE_NAME%' uninstalled successfully!
echo.
echo Note: Log files in the logs\ directory were not deleted.
echo You may manually delete them if desired.
echo.

pause
