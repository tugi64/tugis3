@echo off
cd /d "C:\Users\CASPER\AndroidStudioProjects\tugis3"

echo Restoring LineStakeoutActivity.kt from 2025-10-27 22:40...

REM Get commit hash around that time
for /f "tokens=1" %%i in ('git log --all --since="2025-10-27 22:00" --until="2025-10-27 23:00" --pretty=format:"%%H" --reverse -1') do set COMMIT_HASH=%%i

if "%COMMIT_HASH%"=="" (
    echo No commit found for that time. Trying latest from 2025-10-27...
    for /f "tokens=1" %%i in ('git log --all --since="2025-10-27 00:00" --until="2025-10-27 23:59" --pretty=format:"%%H" -1') do set COMMIT_HASH=%%i
)

if not "%COMMIT_HASH%"=="" (
    echo Found commit: %COMMIT_HASH%
    git show %COMMIT_HASH%:app/src/main/java/com/example/tugis3/ui/survey/LineStakeoutActivity.kt > app\src\main\java\com\example\tugis3\ui\survey\LineStakeoutActivity.kt.restored
    echo File restored to LineStakeoutActivity.kt.restored
    echo Please review and rename if correct.
) else (
    echo No commits found for 2025-10-27
)

pause

