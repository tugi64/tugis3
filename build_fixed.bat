@echo off
echo ===============================================
echo TUGIS3 - Firebase Devre Disi Build
echo ===============================================
echo.

REM Gradle cache temizle
echo [1/4] Gradle cache temizleniyor...
call gradlew.bat clean

REM Build configuration cache temizle
echo.
echo [2/4] Configuration cache temizleniyor...
rmdir /s /q .gradle\configuration-cache 2>nul

REM Debug build yap
echo.
echo [3/4] Debug APK derleniyor...
call gradlew.bat assembleDebug --stacktrace

REM Sonuc
echo.
echo [4/4] Build tamamlandi!
echo.
if exist app\build\outputs\apk\debug\app-debug.apk (
    echo [BASARILI] APK dosyasi olusturuldu:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo Dosya boyutu:
    dir app\build\outputs\apk\debug\app-debug.apk | find "app-debug.apk"
) else (
    echo [HATA] APK dosyasi olusturulamadi!
    echo Loglari kontrol edin.
)

echo.
echo ===============================================
pause

