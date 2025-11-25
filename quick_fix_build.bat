@echo off
echo ========================================
echo Hizli Cache Temizleme
echo ========================================
echo.

cd /d "%~dp0"

echo Gradle daemon durduruluyor...
call gradlew.bat --stop

echo.
echo Build klasorleri temizleniyor...
call gradlew.bat clean

echo.
echo KSP cache temizleniyor...
if exist "app\build\generated\ksp" rd /s /q "app\build\generated\ksp"
if exist "app\build\tmp\kapt3" rd /s /q "app\build\tmp\kapt3"
if exist "app\.ksp" rd /s /q "app\.ksp"

echo.
echo Yeniden build ediliyor...
call gradlew.bat assembleDebug

echo.
echo ========================================
echo Tamamlandi!
echo ========================================
pause

