@echo off
echo ===============================================
echo TUGIS3 - APLIKASYON BUILD
echo ===============================================
echo.

cd /d C:\Users\CASPER\AndroidStudioProjects\tugis3

echo [1/3] Gradle daemon durduruluyor...
call gradlew.bat --stop
timeout /t 2 /nobreak >nul

echo [2/3] Cache temizleniyor...
call gradlew.bat clean

echo [3/3] APK derleniyor...
call gradlew.bat assembleDebug --stacktrace

echo.
if exist app\build\outputs\apk\debug\app-debug.apk (
    echo [BASARILI] APK olusturuldu!
    echo.
    echo Konum: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo APK yukleme komutu:
    echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
) else (
    echo [HATA] APK olusturulamadi!
)

echo.
echo ===============================================
pause

