@echo off
cls
echo ============================================
echo   CAD DOSYA AÇMA SORUNU - DUZELTME
echo ============================================
echo.
echo Yapilacaklar:
echo   1. Build temizleme
echo   2. Debug APK olusturma
echo   3. APK telefona yukleme
echo.
pause

echo.
echo [1/3] Build temizleniyor...
cd /d "%~dp0"
call gradlew clean

echo.
echo [2/3] Debug APK olusturuluyor...
call gradlew assembleDebug

if errorlevel 1 (
    echo.
    echo ============================================
    echo   HATA: Build basarisiz!
    echo ============================================
    echo.
    echo Loglar:
    type app\build\reports\*.txt 2>nul
    pause
    exit /b 1
)

echo.
echo [3/3] APK telefona yukleniyor...
adb devices
echo.
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk

if errorlevel 1 (
    echo.
    echo ============================================
    echo   UYARI: APK yuklenemedi!
    echo ============================================
    echo.
    echo Cozum:
    echo   1. Telefonu USB ile baglayın
    echo   2. USB debugging açık mı kontrol edin
    echo   3. ADB sürücüleri yüklü mü kontrol edin
    echo.
    echo Elle kurmak için:
    echo   app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
    echo   dosyasını telefona kopyalayın ve kurun.
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   BASARILI!
echo ============================================
echo.
echo Simdi test edin:
echo   1. Uygulamayi acin
echo   2. CAD Aplikasyon'a gidin
echo   3. "Dosya Ac" butonuna basin
echo   4. DXF dosyasi secin
echo.
echo Beklenen:
echo   - DWG secerseniz: Uyari mesaji
echo   - DXF secerseniz: Yuklenecek, katmanlar gorunecek
echo   - "Proje limiti bul" calısacak
echo.
echo Debug loglari:
echo   adb logcat -s CAD:* DXF:*
echo.
pause

