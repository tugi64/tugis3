@echo off
echo ============================================
echo KSP HATASINI TAM COZUM
echo ============================================
echo.

cd /d "%~dp0"

echo [ADIM 1/6] Bozuk Java dosyalarini silme...
if exist app\src\main\java\com\MainActivity.java (
    del /q app\src\main\java\com\MainActivity.java
    echo    - MainActivity.java silindi
)
if exist app\src\main\java\com\survey (
    rmdir /s /q app\src\main\java\com\survey
    echo    - com.survey paketi silindi
)
if exist app\src\main\java\com\tools (
    rmdir /s /q app\src\main\java\com\tools
    echo    - com.tools paketi silindi
)
if exist app\src\main\java\com\project (
    rmdir /s /q app\src\main\java\com\project
    echo    - com.project paketi silindi
)
if exist app\src\main\java\com\device (
    rmdir /s /q app\src\main\java\com\device
    echo    - com.device paketi silindi
)

echo.
echo [ADIM 2/6] KSP cache temizleniyor...
if exist app\build\kspCaches (
    rmdir /s /q app\build\kspCaches
    echo    KSP cache silindi
)

echo.
echo [ADIM 3/6] Build klasorleri temizleniyor...
if exist app\build rmdir /s /q app\build
if exist core\data\build rmdir /s /q core\data\build
if exist core\ui\build rmdir /s /q core\ui\build
if exist core\cad\build rmdir /s /q core\cad\build
echo    Tum build klasorleri temizlendi

echo.
echo [ADIM 4/6] .gradle cache temizleniyor...
if exist .gradle (
    rmdir /s /q .gradle
    echo    .gradle cache silindi
)

echo.
echo [ADIM 5/6] Gradle daemon durduruluyor...
call gradlew --stop
timeout /t 3 >nul

echo.
echo [ADIM 6/6] Clean build yapiliyor...
call gradlew clean --no-daemon

echo.
echo ============================================
echo TAMAMLANDI!
echo ============================================
echo.
echo Simdi build yapmak icin:
echo.
echo    gradlew assembleDebug --no-daemon
echo.
echo veya
echo.
echo    gradlew assembleRelease --no-daemon
echo.
pause

