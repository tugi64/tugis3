@echo off
cd /d C:\Users\CASPER\AndroidStudioProjects\tugis3
echo === Git Status ===
git status
echo.
echo === Son 5 commiti gösteriyorum ===
git log --oneline -5
echo.
echo === Son 5 dakikadaki değişiklikler ===
git log --oneline --since="5 minutes ago"
echo.
echo === Değiştirilmiş dosyalar ===
git diff --name-only HEAD

