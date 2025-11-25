@echo off
chcp 65001 >nul
echo.
echo ========================================
echo   APK YÜKLEME HIZLANDIRMA
echo ========================================
echo.

REM Eski APK'yı kaldır
echo [1/4] Eski APK kaldırılıyor...
adb uninstall com.example.tugis3 2>nul
timeout /t 2 >nul

REM ADB sunucusunu yeniden başlat
echo [2/4] ADB yeniden başlatılıyor...
adb kill-server
timeout /t 1 >nul
adb start-server
timeout /t 2 >nul

REM Cihazı kontrol et
echo [3/4] Cihaz kontrol ediliyor...
adb devices

REM Yeni APK'yı yükle
echo [4/4] Yeni APK yükleniyor...
echo.
set APK_FILE=app\build\outputs\apk\debug\app-arm64-v8a-debug.apk

if not exist "%APK_FILE%" (
    echo ❌ APK dosyası bulunamadı: %APK_FILE%
    echo.
    echo Önce build yapmalısın:
    echo   gradlew assembleDebug --no-daemon
    pause
    exit /b 1
)

echo APK: %APK_FILE%
echo.
adb install -r -d -t "%APK_FILE%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   ✅ APK BAŞARIYLA YÜKLENDİ!
    echo ========================================
    echo.
    echo Uygulamayı başlatmak için:
    adb shell am start -n com.example.tugis3/.ui.navigation.MainNavigationActivity
) else (
    echo.
    echo ========================================
    echo   ❌ APK YÜKLEME HATASI
    echo ========================================
    echo.
    echo Çözümler:
    echo 1. USB hata ayıklama açık olsun
    echo 2. Bilgisayarda "Dosya aktarımı" seçili olsun
    echo 3. Telefonda "Bu bilgisayara güven" onaylı olsun
    echo 4. Ekranda kilidi olmamalı
)

echo.
pause

