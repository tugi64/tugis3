@echo off
echo ============================================
echo Bluetooth Eslestirme Guncellemesi Build
echo ============================================
echo.
echo Projeyi derliyoruz...
echo.

cd /d "%~dp0"

call gradlew clean
call gradlew assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo BUILD BASARILI!
    echo ============================================
    echo.
    echo APK konumu:
    echo %~dp0app\build\outputs\apk\debug\
    echo.
    echo Degisiklikler:
    echo - Bluetooth eslestirme duzeltildi
    echo - Bond state receiver eklendi
    echo - Baglanti bildirimleri eklendi
    echo - CadViewModel.kt duzeltildi
    echo.
) else (
    echo.
    echo ============================================
    echo BUILD HATASI!
    echo ============================================
    echo.
    echo Loglari kontrol edin.
    echo.
)

pause

