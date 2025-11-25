@echo off
REM TUGIS3 - Emulator Tamamen Yeniden Baslat
echo ===============================================
echo EMULATOR FULL RESTART - Google Sign In Donmasi Cozumu
echo ===============================================
echo.

set ANDROID_HOME=C:\Users\CASPER\AppData\Local\Android\Sdk

echo [1/5] Mevcut emulator'leri buluyorum...
for /f "tokens=1" %%a in ('"%ANDROID_HOME%\platform-tools\adb.exe" devices ^| findstr "emulator"') do (
    echo Bulunan emulator: %%a
    echo Kapatiliyor...
    "%ANDROID_HOME%\platform-tools\adb.exe" -s %%a emu kill
    timeout /t 2 /nobreak >nul
)

echo.
echo [2/5] ADB sunucusunu sifirliyorum...
"%ANDROID_HOME%\platform-tools\adb.exe" kill-server
timeout /t 3 /nobreak >nul
"%ANDROID_HOME%\platform-tools\adb.exe" start-server
timeout /t 3 /nobreak >nul

echo.
echo [3/5] Tum emulator process'lerini kontrol ediyorum...
tasklist | findstr "qemu-system"
taskkill /F /IM qemu-system-x86_64.exe 2>nul
taskkill /F /IM emulator.exe 2>nul
timeout /t 2 /nobreak >nul

echo.
echo [4/5] Temizlik tamamlandi!
echo.
echo ===============================================
echo SIMDI YAPMANIZ GEREKENLER:
echo ===============================================
echo.
echo 1. Android Studio'yu acin
echo 2. Tools ^> Device Manager
echo 3. Bir emulator secin
echo 4. Play (▶) butonuna basin
echo 5. Emulator tamamen acilana kadar bekleyin (30-60 saniye)
echo 6. Ana ekrani gorunce bu pencereye donun
echo.
echo ===============================================
echo Emulator acildiktan sonra devam etmek icin:
pause

echo.
echo [5/5] Emulator baglantisini kontrol ediyorum...
"%ANDROID_HOME%\platform-tools\adb.exe" devices -l

echo.
echo ===============================================
"%ANDROID_HOME%\platform-tools\adb.exe" devices | findstr "device" >nul
if %ERRORLEVEL% EQU 0 (
    echo [BASARILI] Emulator baglandi!
    echo.
    echo Simdi Google Sign In sayfasini KAPATIN
    echo Sonra build yapabilirsiniz:
    echo   build_fixed.bat
) else (
    echo [HATA] Emulator hala bagli degil!
    echo Emulator'un tamamen acildigina emin olun.
)

echo.
echo ===============================================
pause

