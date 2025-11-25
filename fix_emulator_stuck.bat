@echo off
echo ===============================================
echo TUGIS3 - Emulator Stuck Sorunu Cozumu
echo ===============================================
echo.

set ANDROID_HOME=C:\Users\CASPER\AppData\Local\Android\Sdk

echo [1/6] ADB sunucusunu durduruyorum...
"%ANDROID_HOME%\platform-tools\adb.exe" kill-server
timeout /t 2 /nobreak >nul

echo [2/6] ADB sunucusunu yeniden baslatiyorum...
"%ANDROID_HOME%\platform-tools\adb.exe" start-server
timeout /t 3 /nobreak >nul

echo [3/6] Bagli cihazlari kontrol ediyorum...
"%ANDROID_HOME%\platform-tools\adb.exe" devices -l

echo.
echo [4/6] Emulator durumunu kontrol ediyorum...
for /f "tokens=1,2" %%a in ('"%ANDROID_HOME%\platform-tools\adb.exe" devices ^| findstr "emulator"') do (
    echo Bulunan: %%a - Durum: %%b
    if "%%b"=="offline" (
        echo [UYARI] Emulator OFFLINE! Yeniden baslatiliyor...
        "%ANDROID_HOME%\platform-tools\adb.exe" -s %%a reboot
        timeout /t 5 /nobreak >nul
    )
    if "%%b"=="device" (
        echo [BASARILI] Emulator ONLINE!
    )
)

echo.
echo [5/6] 5 saniye bekliyorum...
timeout /t 5 /nobreak >nul

echo [6/6] Final durum kontrolu...
"%ANDROID_HOME%\platform-tools\adb.exe" devices -l

echo.
echo ===============================================
echo SONUC:
echo ===============================================
"%ANDROID_HOME%\platform-tools\adb.exe" devices | findstr "device" >nul
if %ERRORLEVEL% EQU 0 (
    echo [BASARILI] Emulator hazir!
    echo.
    echo Simdi build yapabilirsiniz:
    echo   build_fixed.bat
) else (
    echo [HATA] Emulator hala bagli degil!
    echo.
    echo Manuel cozum:
    echo 1. Android Studio'dan emulator'u KAPATIN
    echo 2. 10 saniye bekleyin
    echo 3. Emulator'u yeniden BASLATINT
    echo 4. Bu script'i tekrar calistirin
)

echo.
echo ===============================================
pause

