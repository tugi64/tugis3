@echo off
echo ======================================================================
echo Local History Nasil Kullanilir?
echo ======================================================================
echo.
echo ADIM 1: Android Studio'yu acin
echo.
echo ADIM 2: LineStakeoutActivity.kt dosyasini bulun:
echo         app/src/main/java/com/example/tugis3/ui/survey/LineStakeoutActivity.kt
echo.
echo ADIM 3: Dosyaya SAG TIKLAYIN
echo.
echo ADIM 4: Menuden secin:
echo         "Local History" -^> "Show History"
echo.
echo ADIM 5: Acilan pencerede:
echo         - Sag tarafta zaman cizelgesi goreceksiniz
echo         - 27 Ekim 2025, saat 22:30-22:45 arasi bir degisiklik arayin
echo         - Her degisiklige tiklayinca sol tarafta icerigi gorebilirsiniz
echo.
echo ADIM 6: Dogru versiyonu bulunca:
echo         - Ustteki "Revert" butonuna tiklayin
echo         - Veya versiyona sag playtime "Revert" secin
echo.
echo ======================================================================
echo.
echo Eger Local History'de bulamazsan:
echo   - Bu klasorde python restore_from_last_commit.py komutunu calistir
echo   - Bu, git'teki en son calisir versiyonu geri getirir (8 Ekim)
echo.
echo ======================================================================
echo.
pause

