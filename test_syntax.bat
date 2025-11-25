@echo off
REM TUGIS3 - Hizli Test Build
REM Bu script sadece syntax hatasinin duzeltildigini test eder

echo ===============================================
echo TUGIS3 - Syntax Testi
echo ===============================================
echo.

cd /d C:\Users\CASPER\AndroidStudioProjects\tugis3

echo [1/2] Gradle dosyalarini kontrol ediliyor...
call gradlew.bat projects

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [BASARILI] Gradle dosyalari gecerli!
    echo.
    echo [2/2] Simdi tam build yapabilirsiniz:
    echo   build_fixed.bat
) else (
    echo.
    echo [HATA] Gradle dosyalari hatali!
    echo Hata logunu kontrol edin.
)

echo.
echo ===============================================
pause

