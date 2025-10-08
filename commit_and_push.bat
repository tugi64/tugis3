@echo off
setlocal enableextensions enabledelayedexpansion
REM commit_and_push.bat - Degisiklikleri otomatik olarak mevcut dala (master/main vs.) commit ve push eder.
SET REPO_DIR=C:\Users\CASPER\AndroidStudioProjects\tugis3
cd /d "%REPO_DIR%" || (echo Repo dizinine gidilemedi & exit /b 1)

echo === Repo: %REPO_DIR% ===

REM ---------------- Commit Mesaji Derleme (Basit, guvenilir) ----------------
REM Not: Parametreleri tırnakla verirsen ("Mesaj metni"), %* zaten tirnaksiz gelir.
if "%~1"=="" (
    set "COMMIT_MSG=Update project changes"
) else (
    set "COMMIT_MSG=%*"
)
REM Dikkat: ! karakteri commit mesajinda varsa delayed expansion nedeniyle kaybolabilir.
REM Cok gerekirse bu script basina setlocal disabledelayedexpansion getirip sadece burada yeniden enable yapilabilir.

echo Commit message: !COMMIT_MSG!

REM ---------------- Dal Tespiti ----------------
for /f "delims=" %%b in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set CURRENT_BRANCH=%%b
if not defined CURRENT_BRANCH (
    echo HATA: Dal adi alinamadi (detached HEAD olabilir). Varsayilan master denenecek.
    set CURRENT_BRANCH=master
)
if /I "!CURRENT_BRANCH!"=="HEAD" (
    echo UYARI: Detached HEAD durumundasin. master dalina geciliyor.
    git checkout master 1>nul 2>nul && set CURRENT_BRANCH=master
)

echo Aktif dal: !CURRENT_BRANCH!

git status --short

REM ---------------- Build Klasorlerini Index'ten Temizle ----------------
for %%P in (app\build core\cad\build core\data\build core\ui\build build) do (
  if exist "%%P" (
    git ls-files --error-unmatch %%P 1>nul 2>nul && git rm -r --cached "%%P" >nul 2>nul
  )
)

REM ---------------- Degisiklikleri Ekle / Commit ----------------
git add -A
if errorlevel 1 (
    echo HATA: git add basarisiz. & exit /b 1
)

git diff --cached --quiet
if errorlevel 1 (
    git commit -m "!COMMIT_MSG!" || (echo HATA: Commit basarisiz & exit /b 1)
) else (
    echo Commitlenecek degisiklik yok.
)

REM ---------------- Rebase Pull ----------------
echo === Uzak degisiklikler cekiliyor (rebase) ===
git pull origin !CURRENT_BRANCH! --rebase
if errorlevel 1 (
    echo HATA: git pull --rebase basarisiz. Conflict coz -> git add . & git rebase --continue veya iptal icin git rebase --abort
    goto end
)

REM ---------------- Push ----------------
echo === Push ===
git push origin !CURRENT_BRANCH!
if errorlevel 1 (
    echo HATA: git push basarisiz. Kimlik dogrulama / dal korumasi / izinleri kontrol edin.
    goto end
)

echo Bitti. Dal: !CURRENT_BRANCH!

:end
pause
endlocal
