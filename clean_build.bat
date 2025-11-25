@echo off
cd /d C:\Users\CASPER\AndroidStudioProjects\tugis3
echo Cleaning build...
call gradlew.bat clean
echo.
echo Rebuilding...
call gradlew.bat assembleDebug
echo Done!
pause

