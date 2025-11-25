@echo off
cd /d C:\Users\CASPER\AndroidStudioProjects\tugis3

echo ============================================
echo QUICK BUILD TEST - After KSP Fix
echo ============================================
echo.

echo [1/2] Cleaning KSP caches...
if exist "app\build\kspCaches" rmdir /s /q "app\build\kspCaches"
if exist "app\build\generated\ksp" rmdir /s /q "app\build\generated\ksp"

echo [2/2] Building debug APK...
call gradlew assembleDebug

echo.
echo ============================================
if %ERRORLEVEL% EQU 0 (
    echo ✅ BUILD SUCCESSFUL!
    echo APK Location: app\build\outputs\apk\debug\
) else (
    echo ❌ BUILD FAILED
    echo Check error messages above
)
echo ============================================
pause

