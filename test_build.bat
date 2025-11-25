@echo off
echo ====================================
echo BUILD TEST - Optimizasyon Sonrasi
echo ====================================
echo.

cd C:\Users\CASPER\AndroidStudioProjects\tugis3

echo [1/2] Clean build...
call gradlew clean

echo.
echo [2/2] Build assembling...
call gradlew assembleDebug

echo.
echo ====================================
echo BUILD TAMAMLANDI!
echo ====================================
echo.
echo APK'lar:
dir app\build\outputs\apk\debug\*.apk

pause

