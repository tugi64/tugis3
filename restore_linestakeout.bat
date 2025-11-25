@echo off
cd /d "C:\Users\CASPER\AndroidStudioProjects\tugis3"

echo === Git log for LineStakeoutActivity.kt on 2025-10-27 ===
git log --all --since="2025-10-27 20:00" --until="2025-10-27 23:59" --pretty=format:"%%H %%ad %%s" --date=format:"%%Y-%%m-%%d %%H:%%M" -- app/src/main/java/com/example/tugis3/ui/survey/LineStakeoutActivity.kt

echo.
echo.
echo === All commits on 2025-10-27 22:00-23:00 ===
git log --all --since="2025-10-27 22:00" --until="2025-10-27 23:00" --pretty=format:"%%H %%ad %%s" --date=format:"%%Y-%%m-%%d %%H:%%M"

pause

