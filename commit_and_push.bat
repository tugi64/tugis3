@echo off
REM commit_and_push.bat - Degisiklikleri otomatik olarak mevcut dala (master/main vs.) commit ve push eder.
SET REPO_DIR=C:\Users\CASPER\AndroidStudioProjects\tugis3
cd /d "%REPO_DIR%" || (echo Repo dizinine gidilemedi & exit /b 1)

echo === Repo: %REPO_DIR% ===

REM Commit mesaji parametre olarak verilmemisse varsayilan
if "%~1"=="" (
    set COMMIT_MSG=Update project changes
) else (
    set COMMIT_MSG=%*
)

echo Commit message: %COMMIT_MSG%

REM Mevcut dal adini tespit et
for /f "delims=" %%b in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set CURRENT_BRANCH=%%b
if "%CURRENT_BRANCH%"=="" (
    echo HATA: Dal adi alinamadi (detached HEAD olabilir). Varsayilan master denenecek.
    set CURRENT_BRANCH=master
)
if /I "%CURRENT_BRANCH%"=="HEAD" (
    echo UYARI: Detached HEAD durumundasin. master dalina geciliyor.
    git checkout master 2>nul && set CURRENT_BRANCH=master
)

echo Aktif dal: %CURRENT_BRANCH%

git status --short

REM build klasorleri zaten .gitignore'da; izleme disina cikmis ama index'te kalan varsa temizle
FOR %%P IN (app\build core\cad\build core\data\build core\ui\build build) DO (
  if exist "%%P" (
    git ls-files --error-unmatch %%P 1>nul 2>nul && git rm -r --cached %%P >nul 2>nul
  )
)

REM Degisiklikleri ekle
git add -A

REM Staged degisiklik var mi kontrol et
git diff --cached --quiet
if ERRORLEVEL 1 (
    git commit -m "%COMMIT_MSG%" || (echo Commit basarisiz & exit /b 1)
) else (
    echo Commitlenecek degisiklik yok.
)

echo === Uzak degisiklikler cekiliyor (rebase) ===
git pull origin %CURRENT_BRANCH% --rebase
if ERRORLEVEL 1 (
    echo HATA: git pull --rebase basarisiz. Conflict varsa cozumleyip tekrar calistir.
    exit /b 1
)

echo === Push ===
git push origin %CURRENT_BRANCH%
if ERRORLEVEL 1 (
    echo HATA: git push basarisiz. Kimlik dogrulama veya dal korumasi kontrol edin.
    exit /b 1
)

echo Bitti. Dal: %CURRENT_BRANCH%
pause
