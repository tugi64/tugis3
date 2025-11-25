@echo off
echo ============================================
echo BLUETOOTH TARAMA SORUNU DUZELTILDI
echo ============================================
echo.

echo [1] Duplicate dosyalari sil (MANUEL):
echo.
echo    IDE'de su dosyalari silin:
echo    - BluetoothSettingsActivity.kt
echo    - BluetoothSettingsActivity_FIXED.kt
echo.
echo    KULLANILAN:
echo    - BluetoothSettingsActivityNew.kt (YENİ!)
echo.

pause

echo.
echo [2] Clean build...
call gradlew clean

echo.
echo [3] Build aliniyor...
call gradlew assembleDebug

echo.
echo [4] APK konumu:
echo    app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
echo.

echo ============================================
echo ONEMLI: Android 11 ve alti icin
echo ============================================
echo.
echo    KONUM SERVISINI AC!
echo    Ayarlar -^> Konum -^> Ac
echo.
echo    Bu cok onemli! Konum kapali ise
echo    Bluetooth tarama CALISMAZ!
echo.

echo ============================================
echo TEST:
echo ============================================
echo.
echo 1. APK kur
echo 2. Bluetooth Ayarlari ac
echo 3. Bluetooth aç
echo 4. Android 11 alti ise: KONUM AC!
echo 5. "Yeni Cihaz Tara" bas
echo.
echo Sonuc:
echo - "Tarama basladi..." gorunmeli
echo - Cihazlar bulunmali
echo - "X cihaz bulundu" gorunmeli
echo.

pause

