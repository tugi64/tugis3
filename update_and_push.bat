@echo off
REM update_and_push.bat - Güvenli şekilde .idea yedeği alır, uzak URL'yi ayarlar, pull ve push yapar
REM Çalıştırmak için: cmd.exe ile çalıştırın (Admin gerekmez). Proje klasörünüz farklıysa değiştirin.

SET REPO_DIR=C:\Users\CASPER\AndroidStudioProjects\tugis3
cd /d "%REPO_DIR%"

echo === Repo dizinine gidiliyor: %REPO_DIR%

:: Yedekleme: .idea klasörünü masaüstüne taşı
necho Backing up local .idea if exists...
if exist ".idea" (
) else (
    echo ".idea not found, continuing..."
)
if exist ".idea" (
    mkdir "%USERPROFILE%\Desktop\tugis3_idea_backup" 2>nul
    move ".idea" "%USERPROFILE%\Desktop\tugis3_idea_backup" >nul
    echo ".idea moved to %USERPROFILE%\Desktop\tugis3_idea_backup"
)

echo Setting remote origin URL...
git remote set-url origin https://github.com/tugi64/tugis3.git
if ERRORLEVEL 1 (
    echo WARNING: 'git remote set-url' returned an error. Check remote with 'git remote -v'.
)

echo Fetching origin...
git fetch origin

echo Pulling latest changes from origin/main (may prompt for credentials)...
git pull origin main --rebase
if ERRORLEVEL 1 (
    echo ERROR: 'git pull' failed. If output mentioned untracked files, ensure .idea was moved. See README_UPDATE_AND_PUSH.txt for manual steps.
    pause
    exit /b 1
)

echo Adding local changes...
git add -A
git commit -m "Update: DeviceRepository models and layout fixes" 2>nul || echo No changes to commit

echo Pushing to origin/main (may prompt for credentials)...
git push origin main
if ERRORLEVEL 1 (
    echo ERROR: 'git push' failed. Possible authentication issue. Configure your Git credentials or SSH key.
    pause
    exit /b 1
)

echo All done. If you moved .idea, it was backed up to %USERPROFILE%\Desktop\tugis3_idea_backup
pause

