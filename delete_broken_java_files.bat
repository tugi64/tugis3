@echo off
echo ============================================
echo BOZUK JAVA DOSYALARINI SIL
echo ============================================
echo.

cd /d "%~dp0"

echo Bozuk Java dosyalari siliniyor...

REM com paketindeki bozuk dosyalar
if exist app\src\main\java\com\MainActivity.java del /q app\src\main\java\com\MainActivity.java
if exist app\src\main\java\com\survey rmdir /s /q app\src\main\java\com\survey
if exist app\src\main\java\com\tools rmdir /s /q app\src\main\java\com\tools
if exist app\src\main\java\com\project rmdir /s /q app\src\main\java\com\project
if exist app\src\main\java\com\device rmdir /s /q app\src\main\java\com\device

REM Bos com klasoru
if exist app\src\main\java\com (
    dir /b /a app\src\main\java\com | findstr "^" >nul && (
        echo    com klasoru bos degil, koruyoruz
    ) || (
        rmdir /q app\src\main\java\com
        echo    Bos com klasoru silindi
    )
)

echo.
echo Tamamlandi!
echo.
echo Simdi fix_ksp_error.bat dosyasini calistirin.
echo.
pause

