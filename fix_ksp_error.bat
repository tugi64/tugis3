@echo off
echo ============================================
echo KSP ERROR FIX - Cleaning KSP Caches
echo ============================================

cd /d %~dp0

echo [1/4] Stopping Gradle daemons...
call gradlew --stop

echo [2/4] Cleaning project...
call gradlew clean

echo [3/4] Deleting KSP caches...
if exist "app\build\generated\ksp" rmdir /s /q "app\build\generated\ksp"
if exist "app\build\kspCaches" rmdir /s /q "app\build\kspCaches"
if exist ".gradle" rmdir /s /q ".gradle"

echo [4/4] Building debug...
call gradlew assembleDebug --no-daemon --rerun-tasks

echo.
echo ============================================
echo KSP ERROR FIX COMPLETED!
echo ============================================
pause

