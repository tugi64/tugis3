@echo off
echo ============================================
echo TUGIS3 - HIZLI BUILD
echo ============================================
echo.

cd /d "%~dp0"

echo [1/2] Clean yapiliyor...
call gradlew clean >nul 2>&1

echo [2/2] Debug APK derleniyor...
call gradlew assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo ✅ BUILD BASARILI!
    echo ============================================
    echo.
    echo APK Konumu:
    echo %~dp0app\build\outputs\apk\debug\
    echo.
    echo Dosyalar:
    dir /B "%~dp0app\build\outputs\apk\debug\*.apk" 2>nul
    echo.
    echo Onerilen APK: app-arm64-v8a-debug.apk
    echo.
) else (
    echo.
    echo ============================================
    echo ❌ BUILD HATASI!
    echo ============================================
    echo.
    echo Hatalari yukarida kontrol edin.
    echo.
)

pause

