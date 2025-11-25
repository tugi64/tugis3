@echo off
echo Checking for dependency updates...
call gradlew.bat dependencyUpdates
echo.
echo Dependency update check completed!
echo Check build/dependencyUpdates/report.txt for details.
pause

