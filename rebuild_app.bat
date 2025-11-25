@echo off
cd /d C:\Users\CASPER\AndroidStudioProjects\tugis3
echo Hat aplikasyon ekrani onceki haline getirildi.
echo.
echo Simdi clean build yapiliyor...
echo.
call gradlew.bat clean
echo.
echo Debug build yapiliyor...
call gradlew.bat assembleDebug
echo.
echo Tamamlandi!
pause

