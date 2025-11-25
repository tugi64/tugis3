@echo off
echo ============================================
echo COMPLETE BUILD FIX - TUM SORUNLARI COZUYOR
echo ============================================
echo.

cd /d "%~dp0"

echo [1/6] Duplicate dosyalar temizleniyor...
del /f /q "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity.kt" 2>nul
del /f /q "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity_FIXED.kt" 2>nul
del /f /q "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity_NEW.kt" 2>nul
del /f /q "app\src\main\java\com\example\tugis3\ui\survey\PointSurveyActivity_NEW.kt" 2>nul
del /f /q "app\src\main\java\com\example\tugis3\ui\survey\CrossSectionMeasurementActivity_NEW.kt" 2>nul

echo [2/6] BluetoothSettingsActivityNew.kt ismi duzeltiliyor...
ren "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivityNew.kt" "BluetoothSettingsActivity.kt" 2>nul

echo [3/6] Gradle cache temizleniyor...
rmdir /s /q ".gradle" 2>nul
rmdir /s /q "build" 2>nul
rmdir /s /q "app\build" 2>nul
rmdir /s /q "core\data\build" 2>nul
rmdir /s /q "core\ui\build" 2>nul
rmdir /s /q "core\cad\build" 2>nul

echo [4/6] KSP cache temizleniyor...
rmdir /s /q "app\build\kspCaches" 2>nul
rmdir /s /q ".gradle\kotlin" 2>nul

echo [5/6] Gradle daemon durduruluyor...
call gradlew --stop

echo [6/6] Build baslatiliyor...
echo.
echo ============================================
echo BUILD BASLIYOR - LUTFEN BEKLEYIN...
echo ============================================
echo.

call gradlew assembleDebug --no-daemon --info

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo BUILD BASARILI! ✅
    echo ============================================
    echo.
    echo APK Konumu:
    echo app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
    echo.
    echo Telefona Yuklemek icin:
    echo adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
    echo.
) else (
    echo.
    echo ============================================
    echo BUILD HATALI! ❌
    echo ============================================
    echo.
    echo Loglari kontrol edin.
    echo.
)

pause

