@echo off
chcp 65001 >nul
echo.
echo ========================================
echo   BUILD ve YÜKLE (TEK KOMUT)
echo ========================================
echo.

REM Clean build
echo [1/3] Clean build yapılıyor...
call gradlew clean --no-daemon --quiet
if %ERRORLEVEL% NEQ 0 goto :error

REM Debug APK oluştur
echo [2/3] Debug APK oluşturuluyor...
call gradlew assembleDebug --no-daemon
if %ERRORLEVEL% NEQ 0 goto :error

REM APK'yı yükle
echo [3/3] APK yükleniyor...
call quick_install.bat
goto :end

:error
echo.
echo ❌ Build hatası! Loglara bak:
echo   app\build\reports\
pause
exit /b 1

:end

