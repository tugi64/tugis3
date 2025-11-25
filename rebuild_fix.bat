@echo off
echo ============================================
echo BUILD FIX - Clean + Rebuild
echo ============================================

echo [1/3] Cleaning KSP caches...
call clean_ksp_caches.bat

echo [2/3] Invalidating Gradle cache...
gradlew --stop
gradlew clean

echo [3/3] Building debug APK...
gradlew assembleDebug --no-daemon --info

echo ============================================
echo BUILD COMPLETED
echo ============================================
pause

