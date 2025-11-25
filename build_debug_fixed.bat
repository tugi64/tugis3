@echo off
echo ========================================
echo    TUGIS3 DEBUG BUILD
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Gradle temizligi...
call gradlew clean --no-daemon

echo.
echo [2/3] Debug APK olusturuluyor...
call gradlew assembleDebug --no-daemon

echo.
echo [3/3] APK konumu:
echo %CD%\app\build\outputs\apk\debug\
echo.

dir /b app\build\outputs\apk\debug\*.apk 2>nul
if errorlevel 1 (
    echo.
    echo [HATA] APK olusturulamadi!
    echo Build loglarini kontrol edin.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   BUILD BASARILI!
echo ========================================
echo.
echo APK'yi telefona yuklemeye hazir.
echo ADB ile yuklemek icin:
echo   adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
echo.
pause

