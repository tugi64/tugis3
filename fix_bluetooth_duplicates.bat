@echo off
echo ============================================
echo BLUETOOTH DUPLICATE DOSYALARI TEMIZLENIYOR
echo ============================================
echo.

cd /d "%~dp0"

echo [1] Eski Bluetooth dosyalari siliniyor...
del /f /q "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity.kt" 2>nul
del /f /q "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity_FIXED.kt" 2>nul
del /f /q "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity_NEW.kt" 2>nul

echo [2] BluetoothSettingsActivityNew.kt -> BluetoothSettingsActivity.kt
move /y "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivityNew.kt" "app\src\main\java\com\example\tugis3\ui\device\BluetoothSettingsActivity.kt"

echo [3] PointSurveyActivity_NEW.kt temizleniyor...
del /f /q "app\src\main\java\com\example\tugis3\ui\survey\PointSurveyActivity_NEW.kt" 2>nul

echo [4] CrossSectionMeasurementActivity_NEW.kt temizleniyor...
del /f /q "app\src\main\java\com\example\tugis3\ui\survey\CrossSectionMeasurementActivity_NEW.kt" 2>nul

echo [5] KSP cache temizleniyor...
rmdir /s /q "app\build\kspCaches" 2>nul
rmdir /s /q ".gradle\kotlin\errors" 2>nul

echo.
echo ============================================
echo TEMIZLIK TAMAMLANDI!
echo Simdi build yapin: gradlew assembleDebug --no-daemon
echo ============================================
pause

