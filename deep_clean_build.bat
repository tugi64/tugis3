@echo off
echo ========================================
echo Gradle Cache Temizleme ve Yeniden Build
echo ========================================
echo.
echo Adim 1: Build ve cache klasorlerini temizliyorum...
echo.

cd /d "%~dp0"

echo Cleaning build directories...
if exist "build" rd /s /q "build"
if exist "app\build" rd /s /q "app\build"
if exist "core\data\build" rd /s /q "core\data\build"
if exist "core\ui\build" rd /s /q "core\ui\build"
if exist "core\cad\build" rd /s /q "core\cad\build"
if exist "baselineprofile\build" rd /s /q "baselineprofile\build"
if exist "benchmark\build" rd /s /q "benchmark\build"

echo.
echo Cleaning .gradle cache...
if exist ".gradle" rd /s /q ".gradle"

echo.
echo Adim 2: Gradle clean komutu...
call gradlew.bat clean

echo.
echo Adim 3: KSP cache temizleniyor...
if exist "app\.ksp" rd /s /q "app\.ksp"
if exist "app\kspCaches" rd /s /q "app\kspCaches"

echo.
echo Adim 4: Gradle sync ve build...
call gradlew.bat --stop
call gradlew.bat clean build --no-daemon --refresh-dependencies

echo.
echo ========================================
echo Tamamlandi!
echo ========================================
pause

