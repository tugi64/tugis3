# TUGIS3 - Emulator Yardim Script'i
# Bu script PowerShell'de calistirilmalidir

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "TUGIS3 - Emulator Durum Kontrolu" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""

# Android Home ayarla
$Env:ANDROID_HOME = 'C:\Users\CASPER\AppData\Local\Android\Sdk'

Write-Host "[1/5] ADB sunucusu yeniden baslatiliyor..." -ForegroundColor Yellow
& "$Env:ANDROID_HOME\platform-tools\adb.exe" kill-server
Start-Sleep -Seconds 2
& "$Env:ANDROID_HOME\platform-tools\adb.exe" start-server
Start-Sleep -Seconds 2

Write-Host ""
Write-Host "[2/5] Bagli cihazlar kontrol ediliyor..." -ForegroundColor Yellow
& "$Env:ANDROID_HOME\platform-tools\adb.exe" devices -l

Write-Host ""
Write-Host "[3/5] Emulator durumu:" -ForegroundColor Yellow
$devices = & "$Env:ANDROID_HOME\platform-tools\adb.exe" devices | Select-String "emulator"

if ($devices) {
    $deviceStatus = $devices -split "\s+"
    if ($deviceStatus -contains "offline") {
        Write-Host "  [UYARI] Emulator OFFLINE durumda!" -ForegroundColor Red
        Write-Host "  Cozum: Android Studio'dan emulator'u yeniden baslatın" -ForegroundColor Yellow
    } elseif ($deviceStatus -contains "device") {
        Write-Host "  [BASARILI] Emulator ONLINE!" -ForegroundColor Green
    }
} else {
    Write-Host "  [BILGI] Emulator bulunamadi" -ForegroundColor Yellow
    Write-Host "  Android Studio'dan bir emulator baslatin" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[4/5] Fiziksel cihaz kontrol ediliyor..." -ForegroundColor Yellow
$physicalDevices = & "$Env:ANDROID_HOME\platform-tools\adb.exe" devices | Select-String -Pattern "^\w+\s+device" | Where-Object { $_ -notmatch "emulator" }

if ($physicalDevices) {
    Write-Host "  [BASARILI] Fiziksel cihaz bagli!" -ForegroundColor Green
} else {
    Write-Host "  [BILGI] Fiziksel cihaz bulunamadi" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[5/5] APK yukleme komutu:" -ForegroundColor Yellow
Write-Host "  adb install -r app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan

Write-Host ""
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "Emulator Offline ise yapilacaklar:" -ForegroundColor Yellow
Write-Host "  1. Android Studio > Tools > Device Manager" -ForegroundColor White
Write-Host "  2. Emulator'u STOP edin" -ForegroundColor White
Write-Host "  3. 5 saniye bekleyin" -ForegroundColor White
Write-Host "  4. Emulator'u yeniden START edin" -ForegroundColor White
Write-Host "  5. Bu script'i tekrar calistirin" -ForegroundColor White
Write-Host "===============================================" -ForegroundColor Cyan

Read-Host "Devam etmek icin ENTER'a basin"

