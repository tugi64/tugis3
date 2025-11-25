@echo off
color 0A
title TuGIS3 - Hizli Cozum Menusu

:MENU
cls
echo ============================================
echo   TuGIS3 - HIZLI COZUM MENUSU
echo ============================================
echo.
echo [1] BUILD HATASI COZUMU (Otomatik)
echo [2] Sadece Duplicate Dosyalari Temizle
echo [3] Sadece KSP Cache Temizle
echo [4] Normal Build Yap
echo [5] Clean Build Yap
echo [6] Release APK Olustur
echo [7] APK'yi Telefona Yukle
echo [8] Tum Cache'leri Temizle (Nuclear)
echo [9] Yardim/Dokumantasyon
echo [0] Cikis
echo.
echo ============================================
set /p choice="Seciminiz (0-9): "

if "%choice%"=="1" goto FIX_ALL
if "%choice%"=="2" goto FIX_DUPLICATES
if "%choice%"=="3" goto FIX_KSP
if "%choice%"=="4" goto BUILD_NORMAL
if "%choice%"=="5" goto BUILD_CLEAN
if "%choice%"=="6" goto BUILD_RELEASE
if "%choice%"=="7" goto INSTALL_APK
if "%choice%"=="8" goto NUCLEAR_CLEAN
if "%choice%"=="9" goto HELP
if "%choice%"=="0" goto EXIT
goto MENU

:FIX_ALL
echo.
echo ============================================
echo OTOMATIK BUILD DUZELTME BASLIYOR...
echo ============================================
call complete_build_fix.bat
pause
goto MENU

:FIX_DUPLICATES
echo.
echo ============================================
echo DUPLICATE DOSYALAR TEMIZLENIYOR...
echo ============================================
call fix_bluetooth_duplicates.bat
pause
goto MENU

:FIX_KSP
echo.
echo ============================================
echo KSP CACHE TEMIZLENIYOR...
echo ============================================
call clean_ksp_caches.bat
pause
goto MENU

:BUILD_NORMAL
echo.
echo ============================================
echo NORMAL BUILD BASLIYOR...
echo ============================================
call gradlew assembleDebug
pause
goto MENU

:BUILD_CLEAN
echo.
echo ============================================
echo CLEAN BUILD BASLIYOR...
echo ============================================
call gradlew clean assembleDebug --no-daemon
pause
goto MENU

:BUILD_RELEASE
echo.
echo ============================================
echo RELEASE APK OLUSTURULUYOR...
echo ============================================
call gradlew assembleRelease
pause
goto MENU

:INSTALL_APK
echo.
echo ============================================
echo APK TELEFONA YUKLENIYOR...
echo ============================================
echo.
echo Telefon USB ile bagli mi? (Evet/Hayir)
set /p usb="USB bagli mi? (E/H): "
if /i "%usb%"=="E" (
    echo.
    echo APK yukleniyor...
    adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ✅ APK basariyla yuklendi!
    ) else (
        echo.
        echo ❌ APK yukleme hatasi!
        echo.
        echo Manuel yukleme icin:
        echo 1. app\build\outputs\apk\debug\app-arm64-v8a-debug.apk dosyasini telefona kopyalayin
        echo 2. Dosya yoneticisinden acin
    )
) else (
    echo.
    echo Manuel yukleme icin:
    echo 1. app\build\outputs\apk\debug\app-arm64-v8a-debug.apk dosyasini telefona kopyalayin
    echo 2. Dosya yoneticisinden acin
)
pause
goto MENU

:NUCLEAR_CLEAN
echo.
echo ============================================
echo TUM CACHE'LER TEMIZLENIYOR...
echo ============================================
call nuclear_clean.bat
pause
goto MENU

:HELP
echo.
echo ============================================
echo YARDIM VE DOKUMANTASYON
echo ============================================
echo.
echo Detayli bilgi icin asagidaki dosyalari acin:
echo.
echo [1] BUILD_FIX_README.md       - Hizli baslangic
echo [2] KSP_BUILD_HATASI_COZUM.md - Detayli cozum
echo [3] SONRAKI_ADIMLAR.md        - Test senaryolari
echo.
echo Bu dosyalari metin editoru ile acabilirsiniz.
echo.
pause
goto MENU

:EXIT
echo.
echo Cikiliyor...
exit


