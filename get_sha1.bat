@echo off
echo ========================================
echo SHA-1 Fingerprint Alma Araci
echo ========================================
echo.
echo NOT: Bu araç Firebase veya Google Sign-In icin gereklidir.
echo Firebase devre disi oldugu icin simdilik gerekli degil.
echo.
echo Yine de SHA-1 almak isterseniz devam edin...
echo.
pause
echo.
echo Debug SHA-1 Fingerprint aliniyor...
echo.

gradlew.bat signingReport

echo.
echo ========================================
echo Yukaridaki ciktida "SHA-1:" yazan satiri kopyalayin
echo Firebase Console'da "Project Settings > Your apps" bolumune ekleyin
echo ========================================
pause

