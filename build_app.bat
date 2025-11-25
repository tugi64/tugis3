@echo off
echo ========================================
echo TUGIS3 Build Script
echo ========================================
echo.

cd /d "%~dp0"

echo Temizleniyor...
call gradlew clean

echo.
echo Release APK olusturuluyor...
call gradlew assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD BASARILI!
    echo ========================================
    echo APK konumu: app\build\outputs\apk\release\
    pause
) else (
    echo.
    echo ========================================
    echo BUILD HATASI!
    echo ========================================
    pause
    exit /b 1
)

