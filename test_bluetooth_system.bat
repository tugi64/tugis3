@echo off
echo ========================================
echo   TUGIS3 - BLUETOOTH TEST SISTEMI
echo ========================================
echo.

echo [TEST 1] Build durumu kontrol ediliyor...
if not exist "app\build.gradle.kts" (
    echo [HATA] app\build.gradle.kts bulunamadi!
    pause
    exit /b 1
)
echo [OK] Build dosyalari mevcut
echo.

echo [TEST 2] Bluetooth dosyalari kontrol ediliyor...
if not exist "app\src\main\java\com\example\tugis3\bluetooth\BluetoothGnssManager.kt" (
    echo [HATA] BluetoothGnssManager.kt bulunamadi!
    pause
    exit /b 1
)
echo [OK] BluetoothGnssManager.kt mevcut
echo.

if not exist "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivityNew.kt" (
    echo [HATA] BluetoothSettingsActivityNew.kt bulunamadi!
    pause
    exit /b 1
)
echo [OK] BluetoothSettingsActivityNew.kt mevcut
echo.

echo [TEST 3] Duplicate dosyalar temizlendi mi?
set DUPLICATE_COUNT=0

if exist "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity.kt" (
    findstr /C:"DELETED" "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity.kt" >nul
    if errorlevel 1 (
        echo [UYARI] BluetoothSettingsActivity.kt hala aktif!
        set /a DUPLICATE_COUNT+=1
    )
)

if exist "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity_NEW.kt" (
    findstr /C:"DELETED" "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity_NEW.kt" >nul
    if errorlevel 1 (
        echo [UYARI] BluetoothSettingsActivity_NEW.kt hala aktif!
        set /a DUPLICATE_COUNT+=1
    )
)

if %DUPLICATE_COUNT% EQU 0 (
    echo [OK] Duplicate dosyalar temizlendi
) else (
    echo [UYARI] %DUPLICATE_COUNT% aktif duplicate dosya var!
)
echo.

echo [TEST 4] AndroidManifest kontrol ediliyor...
findstr /C:"BluetoothSettingsActivityNew" app\src\main\AndroidManifest.xml >nul
if errorlevel 1 (
    echo [HATA] AndroidManifest'te BluetoothSettingsActivityNew kayitli degil!
    pause
    exit /b 1
) else (
    echo [OK] BluetoothSettingsActivityNew manifest'te kayitli
)
echo.

echo [TEST 5] Bluetooth izinleri kontrol ediliyor...
findstr /C:"BLUETOOTH_SCAN" app\src\main\AndroidManifest.xml >nul
if errorlevel 1 (
    echo [UYARI] BLUETOOTH_SCAN izni manifest'te yok! (Android 12+ icin gerekli)
) else (
    echo [OK] BLUETOOTH_SCAN izni mevcut
)

findstr /C:"BLUETOOTH_CONNECT" app\src\main\AndroidManifest.xml >nul
if errorlevel 1 (
    echo [UYARI] BLUETOOTH_CONNECT izni manifest'te yok! (Android 12+ icin gerekli)
) else (
    echo [OK] BLUETOOTH_CONNECT izni mevcut
)
echo.

echo ========================================
echo   TUM TESTLER TAMAMLANDI
echo ========================================
echo.
echo Sonraki adim:
echo   1. build_debug_fixed.bat calistir
echo   2. APK'yi yukle (arm64-v8a)
echo   3. BLUETOOTH_VE_BUILD_DURUM_RAPORU.md oku
echo.
pause

