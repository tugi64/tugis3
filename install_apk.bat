@echo off
echo ============================================
echo APK OLUSTURMA VE YUKLEME YARDIMCISI
echo ============================================
echo.

REM Check if phone is connected
echo [1/5] Telefon baglantisi kontrol ediliyor...
adb devices
echo.
echo Telefon bagli mi? (Yukarida "device" yazmali)
pause

REM Build APK
echo.
echo [2/5] Debug APK olusturuluyor...
call gradlew assembleDebug --no-daemon
if errorlevel 1 (
    echo.
    echo HATA: APK olusturulamadi!
    pause
    exit /b 1
)

REM Find APK
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if not exist "%APK_PATH%" (
    echo.
    echo HATA: APK dosyasi bulunamadi: %APK_PATH%
    pause
    exit /b 1
)

echo.
echo [3/5] APK bulundu: %APK_PATH%
dir "%APK_PATH%"

REM Uninstall old version first
echo.
echo [4/5] Eski surum kaldiriliyor (varsa)...
adb uninstall com.example.tugis3
timeout /t 2 /nobreak >nul

REM Install new APK
echo.
echo [5/5] Yeni APK yukleniyor...
adb install -r "%APK_PATH%"

if errorlevel 1 (
    echo.
    echo HATA: APK yuklenemedi!
    echo.
    echo Cozum onerileri:
    echo   1. USB debugging acik mi?
    echo   2. Telefonda "Bu bilgisayara izin ver" onayladiniz mi?
    echo   3. "adb devices" komutu telefonu goruyor mu?
    echo   4. Telefonun ekrani acik mi?
    pause
    exit /b 1
)

echo.
echo ============================================
echo APK BASARIYLA YUKLENDI!
echo ============================================
echo.
echo Uygulamayi telefondan acabilirsiniz.
echo.
pause

