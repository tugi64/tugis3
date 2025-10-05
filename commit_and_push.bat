@echo off
REM commit_and_push.bat - Tüm değişiklikleri commit edip origin/main'e push eder.
SET REPO_DIR=C:\Users\CASPER\AndroidStudioProjects\tugis3
cd /d "%REPO_DIR%"

echo === Repo dizinine gidiliyor: %REPO_DIR%

:: Kullanıcıdan commit mesajı al (parametre yoksa default)
if "%1"=="" (
    set COMMIT_MSG=Add Bluetooth GNSS manager, device communication, protocols and services
) else (
    set COMMIT_MSG=%*
)

echo Commit message: %COMMIT_MSG%

:: Git durumunu göster
git status --porcelain 2>nul || git status --porcelain

:: Tüm değişiklikleri ekle ve commit yap (değişiklik yoksa bilgilendir)
git add -A
REM Eğer sahnelenmiş (staged) değişiklik varsa commit yap, yoksa bilgilendir
git diff --cached --quiet
if ERRORLEVEL 1 (
    git commit -m "%COMMIT_MSG%"
) else (
    echo No changes to commit
)

echo Pulling remote changes and rebasing...
git pull origin main --rebase
if ERRORLEVEL 1 (
    echo ERROR: 'git pull' failed. Resolve conflicts manually then run this script again.
    pause
    exit /b 1
)

echo Pushing to origin/main...
git push origin main
if ERRORLEVEL 1 (
    echo ERROR: 'git push' failed. Check authentication (PAT/SSH) and remote URL.
    pause
    exit /b 1
)
echo Done.
pause
