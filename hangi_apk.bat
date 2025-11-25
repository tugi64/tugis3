@echo off
echo ============================================
echo TELEFON MIMARISI KONTROL - Hangi APK?
echo ============================================
echo.
echo Telefonunuzu USB ile baglayip ADB debugging acik olsun!
echo.
pause

echo.
echo [Kontrol ediliyor...]
adb shell getprop ro.product.cpu.abi

echo.
echo ============================================
echo SONUC:
echo ============================================
echo.
echo Eger sonuc:
echo   - arm64-v8a      --^> app-arm64-v8a-debug.apk KURUN (ONERILIR)
echo   - armeabi-v7a    --^> app-armeabi-v7a-debug.apk KURUN
echo   - x86_64         --^> app-x86_64-debug.apk KURUN (Emulator)
echo   - x86            --^> app-x86-debug.apk KURUN (Emulator)
echo.
echo ============================================
pause

