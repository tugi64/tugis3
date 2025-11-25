@echo off
echo.
echo ===============================================
echo    TUGIS3 - SONRAKI ADIMLAR
echo ===============================================
echo.
echo [1] Bluetooth Duzeltmesi Test Et
echo     - Cihaz ^> Bluetooth Ayarlari
echo     - Yeni cihaz tara
echo     - GNSS cihazi ile eslesme dene
echo     - "Bluetooth baglandi!" mesaji geldi mi?
echo.
echo [2] Iletisim Menusu Test Et
echo     - Cihaz ^> Iletisim
echo     - Bluetooth sec
echo     - Tara butonuna bas
echo     - Cihazlar listeleniyor mu?
echo.
echo [3] NCN Format Desteği Ekle (YAPILACAK)
echo     - NcnParser.kt olustur
echo     - Tum import diyaloglarina ekle
echo     - Test et
echo.
echo [4] Dosya Yonet Ekrani Tamamla (YAPILACAK)
echo     - FileManagerActivity.kt guncelle
echo     - UI tasarimi yap
echo     - Dosya islemleri ekle
echo.
echo [5] Gezici Menusu Ekle (YAPILACAK)
echo     - RoverSettingsActivity.kt olustur
echo     - Marka/Model dropdown
echo     - Bluetooth baglanti kontrolu
echo.
echo ===============================================
echo    DOKUMANTASYON
echo ===============================================
echo.
echo Asagidaki dosyalari okuyun:
echo.
echo  - SON_DUZELTMELER_OZET.md
echo    (Yapilan duzeltmeler)
echo.
echo  - YENI_OZELLIKLER_TODO.md
echo    (Gelecek ozellikler listesi)
echo.
echo  - PROJE_DURUMU.md
echo    (Proje durum raporu)
echo.
echo ===============================================
echo    BUILD KOMUTLARI
echo ===============================================
echo.
echo Hizli Build:
echo   HIZLI_BUILD.bat
echo.
echo Tam Temizlik:
echo   nuclear_clean.bat
echo   gradlew assembleDebug --no-daemon
echo.
echo Debug Log:
echo   adb logcat ^| findstr "Tugis3"
echo.
echo ===============================================
echo    APK KONUMU
echo ===============================================
echo.
echo Debug APK:
echo   app\build\outputs\apk\debug\
echo.
echo Onerilen APK:
echo   app-arm64-v8a-debug.apk
echo.
echo ===============================================
echo    BILINEN SORUNLAR
echo ===============================================
echo.
echo [!] Saat donuyor (20:20:12)
echo     Gecici Cozum: Uygulamayi yeniden baslat
echo.
echo [!] Iletisim tarama bazen calismıyor
echo     Cozum: Izinleri kontrol et, Bluetooth resetle
echo.
echo [!] APK kurulum uzun suruyor
echo     Cozum: Play Protect'i gecici kapat
echo.
echo ===============================================
echo    YARDIM
echo ===============================================
echo.
echo Sorun yasarsan:
echo   1. Temiz build yap: HIZLI_BUILD.bat
echo   2. Uygulamayi kaldir-kur
echo   3. Izinleri kontrol et
echo   4. Telefonu yeniden baslat
echo   5. Loglari kontrol et: adb logcat
echo.
echo ===============================================
pause

