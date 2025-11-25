@echo off
echo ============================================
echo NUCLEAR CLEAN - EVERYTHING WILL BE DELETED
echo ============================================
echo.
echo This will:
echo   - Stop all Gradle daemons
echo   - Delete ALL build folders
echo   - Delete .gradle folder
echo   - Delete Gradle cache
echo   - Force a complete rebuild
echo.
echo Press CTRL+C to cancel, or
pause

REM Kill all Java processes (Gradle daemons)
echo.
echo [NUCLEAR] Killing all Java/Gradle processes...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak >nul

REM Delete all build directories
echo.
echo [NUCLEAR] Deleting ALL build directories...
for /d /r %%d in (build) do @if exist "%%d" (
    echo   - Deleting %%d
    rmdir /s /q "%%d" 2>nul
)

REM Delete .gradle
echo.
echo [NUCLEAR] Deleting .gradle directory...
if exist ".gradle" (
    rmdir /s /q ".gradle" 2>nul
)

REM Delete .idea caches
echo.
echo [NUCLEAR] Deleting .idea caches...
if exist ".idea\caches" (
    rmdir /s /q ".idea\caches" 2>nul
)

echo.
echo [NUCLEAR] Waiting for file locks to release...
timeout /t 3 /nobreak >nul

REM Clean Gradle
echo.
echo [NUCLEAR] Running Gradle clean...
call gradlew clean --no-configuration-cache --no-daemon --no-build-cache

echo.
echo ============================================
echo NUCLEAR CLEAN COMPLETED!
echo ============================================
echo.
echo NOW RUN THIS:
echo   gradlew assembleDebug --no-daemon
echo.
pause

