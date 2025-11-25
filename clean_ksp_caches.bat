@echo off
echo ============================================
echo DEEP CLEAN: Gradle + KSP + Build
echo ============================================
echo.

REM Stop Gradle Daemon first
echo [1/5] Stopping Gradle Daemon...
call gradlew --stop
timeout /t 2 /nobreak >nul

REM Delete entire build directories
echo.
echo [2/5] Deleting all build directories...
if exist "app\build" (
    echo   - Deleting app\build...
    rmdir /s /q "app\build"
)
if exist "core\data\build" (
    echo   - Deleting core\data\build...
    rmdir /s /q "core\data\build"
)
if exist "core\ui\build" (
    echo   - Deleting core\ui\build...
    rmdir /s /q "core\ui\build"
)
if exist "core\cad\build" (
    echo   - Deleting core\cad\build...
    rmdir /s /q "core\cad\build"
)
if exist "build" (
    echo   - Deleting root build...
    rmdir /s /q "build"
)

REM Delete .gradle cache
echo.
echo [3/5] Deleting .gradle cache...
if exist ".gradle" (
    rmdir /s /q ".gradle"
)

REM Delete gradle cache directory
echo.
echo [4/5] Cleaning Gradle caches in user directory...
if exist "%USERPROFILE%\.gradle\caches" (
    echo   - Cleaning build-cache...
    rmdir /s /q "%USERPROFILE%\.gradle\caches\build-cache-1"
)

REM Run Gradle clean
echo.
echo [5/5] Running Gradle clean...
call gradlew clean --no-configuration-cache --no-daemon

echo.
echo ============================================
echo DEEP CLEAN COMPLETED SUCCESSFULLY!
echo ============================================
echo.
echo Next steps:
echo   1. Close Android Studio if open
echo   2. Run: gradlew assembleDebug --no-daemon
echo.
pause

