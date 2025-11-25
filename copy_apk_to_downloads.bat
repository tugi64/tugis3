@echo off
echo ============================================
echo MANUEL APK KOPYALAMA
echo ============================================
echo.
echo Bu script APK'yi Downloads klasorune kopyalar.
echo Sonra telefondan manuel yukleyebilirsiniz.
echo.

REM Build APK
echo [1/3] Debug APK olusturuluyor...
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
    echo HATA: APK dosyasi bulunamadi!
    pause
    exit /b 1
)

REM Copy to Downloads
echo.
echo [2/3] APK Downloads klasorune kopyalaniyor...
set DEST=%USERPROFILE%\Downloads\tugis3-debug.apk
copy "%APK_PATH%" "%DEST%"

echo.
echo [3/3] APK hazir!
echo.
echo APK Konumu: %DEST%
echo.
echo MANUEL YUKLEME ADIMLARI:
echo ========================================
echo 1. APK'yi telefona kopyalayin (USB/Email/Drive)
echo 2. Telefondan APK'yi acin
echo 3. "Bilinmeyen kaynaklardan yukle" iznini verin
echo 4. Yuklemeyi onaylayin
echo.
pause

