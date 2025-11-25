@echo off
REM ================================================
REM TUGIS3 - ULTIMATE FIX
REM "Waiting for devices" ve Google Sign-In Sorununu Coz
REM ================================================

echo.
echo ███████████████████████████████████████████████
echo █  TUGIS3 - ULTIMATE EMULATOR FIX             █
echo ███████████████████████████████████████████████
echo.

set ANDROID_HOME=C:\Users\CASPER\AppData\Local\Android\Sdk

echo [ADIM 1/8] Mevcut durumu kontrol ediyorum...
echo.
"%ANDROID_HOME%\platform-tools\adb.exe" devices -l
echo.

echo [ADIM 2/8] Tum emulator'leri kapatiyorum...
for /f "tokens=1" %%a in ('"%ANDROID_HOME%\platform-tools\adb.exe" devices ^| findstr "emulator"') do (
    echo   - Kapatiliyor: %%a
    "%ANDROID_HOME%\platform-tools\adb.exe" -s %%a emu kill 2>nul
)
timeout /t 2 /nobreak >nul

echo [ADIM 3/8] Stuck process'leri temizliyorum...
taskkill /F /IM qemu-system-x86_64.exe 2>nul
taskkill /F /IM emulator.exe 2>nul
taskkill /F /IM adb.exe 2>nul
timeout /t 2 /nobreak >nul

echo [ADIM 4/8] ADB sunucusunu sifirliyorum...
"%ANDROID_HOME%\platform-tools\adb.exe" kill-server
timeout /t 3 /nobreak >nul
"%ANDROID_HOME%\platform-tools\adb.exe" start-server
timeout /t 3 /nobreak >nul

echo [ADIM 5/8] Gradle daemon'u durduruyor...
call gradlew.bat --stop 2>nul
timeout /t 2 /nobreak >nul

echo [ADIM 6/8] Temizlik tamamlandi!
echo.
echo ================================================
echo         KRITIK TALIMATLAR
echo ================================================
echo.
echo 1. Android Studio'yu ACIN (kapali ise)
echo.
echo 2. Tools ^> Device Manager'a gidin
echo.
echo 3. Bir emulator SECIN ve BASLATINT
echo    (Pixel 5 API 34 onerilir)
echo.
echo 4. Ana ekran GORUNENE kadar bekleyin
echo    (30-60 saniye surebilir)
echo.
echo 5. Hazir oldugunda bu pencereye DONUN
echo.
echo ================================================
echo.

set /p dummy="Emulator acildi mi? (ENTER'a basin): "

echo.
echo [ADIM 7/8] Emulator baglantisini test ediyorum...
timeout /t 3 /nobreak >nul
"%ANDROID_HOME%\platform-tools\adb.exe" devices -l
echo.

echo [ADIM 8/8] Durum raporu...
echo.

REM Emulator kontrolu
set DEVICE_FOUND=0
for /f "tokens=1,2" %%a in ('"%ANDROID_HOME%\platform-tools\adb.exe" devices ^| findstr "emulator"') do (
    if "%%b"=="device" (
        echo ✓ BASARILI: Emulator baglandi! [%%a]
        set DEVICE_FOUND=1
    ) else (
        echo ✗ HATA: Emulator %%b durumunda [%%a]
    )
)

echo.
echo ================================================
if %DEVICE_FOUND%==1 (
    echo          BUILD YAPABILIRSINIZ!
    echo ================================================
    echo.
    echo Simdi calistirin:
    echo   build_fixed.bat
    echo.
    echo Sonra APK yukleyin:
    echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
) else (
    echo           EMULATOR HAZIR DEGIL
    echo ================================================
    echo.
    echo Sorun devam ediyorsa:
    echo.
    echo 1. Android Studio'da emulator'u KAPATIN
    echo 2. 10 saniye bekleyin
    echo 3. Emulator'u yeniden BASLATINT
    echo 4. Bu script'i TEKRAR calistirin:
    echo      ultimate_fix.bat
)

echo.
echo ================================================
pause

