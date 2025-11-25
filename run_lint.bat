@echo off
echo Running Android Lint on all modules...
call gradlew.bat lintAll
if errorlevel 1 (
    echo.
    echo Lint checks failed! Please fix the issues.
    pause
    exit /b 1
)
echo.
echo Lint checks completed successfully!
pause

