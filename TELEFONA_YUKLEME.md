# TUGIS3 - Build ve Yükleme Talimatları

## 📱 Uygulamayı Telefona Yükleme

### Yöntem 1: Android Studio ile Doğrudan Yükleme
1. Android Studio'da projeyi açın
2. Telefonunuzu USB ile bilgisayara bağlayın
3. Telefonda **USB Hata Ayıklama** (USB Debugging) açık olmalı
4. Android Studio'da üstteki **▶ Run** butonuna tıklayın
5. Telefonunuzu seçin ve **OK**'a tıklayın

### Yöntem 2: APK ile Yükleme

#### A. APK Oluşturma:
```batch
# Windows Terminal veya PowerShell'de:
cd C:\Users\CASPER\AndroidStudioProjects\tugis3

# Temizle ve Release APK oluştur
.\gradlew clean assembleRelease
```

#### B. APK Konumu:
```
C:\Users\CASPER\AndroidStudioProjects\tugis3\app\build\outputs\apk\release\app-release.apk
```

#### C. Telefona Yükleme:
1. APK dosyasını telefona kopyalayın (USB, Bluetooth veya E-posta ile)
2. Telefonda **Dosya Yöneticisi** uygulamasını açın
3. APK dosyasını bulun ve tıklayın
4. **Bilinmeyen kaynaklardan yükleme** iznini verin (gerekirse)
5. **Yükle** butonuna tıklayın

## 🔧 Build Sorunları ve Çözümleri

### Sorun 1: "Compilation error"
**Çözüm:**
```batch
# Clean build yapın
.\gradlew clean
.\gradlew assembleDebug
```

### Sorun 2: "Duplicate class" hatası
**Çözüm:**
```batch
# Build cache'i temizleyin
.\gradlew clean
.\gradlew cleanBuildCache
.\gradlew assembleRelease
```

### Sorun 3: "SDK location not found"
**Çözüm:**
1. `local.properties` dosyasını kontrol edin
2. SDK path'i doğru olmalı:
```properties
sdk.dir=C\:\\Users\\CASPER\\AppData\\Local\\Android\\Sdk
```

### Sorun 4: "Manifest merger failed"
**Çözüm:**
- AndroidManifest.xml'de duplicate activity tanımı var mı kontrol edin
- Her activity yalnızca bir kez tanımlanmalı

## 📋 Build Öncesi Kontrol Listesi

- [x] Android Studio güncel (Arctic Fox veya üzeri)
- [x] JDK 11 veya üzeri yüklü
- [x] Android SDK 34 yüklü
- [x] Gradle sync tamamlandı
- [x] AndroidManifest.xml'de duplicate tanım yok

## 🚀 Hızlı Build Komutları

### Debug APK (Hızlı Test İçin):
```batch
.\gradlew assembleDebug
```
**Konum:** `app\build\outputs\apk\debug\app-debug.apk`

### Release APK (Dağıtım İçin):
```batch
.\gradlew assembleRelease
```
**Konum:** `app\build\outputs\apk\release\app-release.apk`

### Clean Build (Sorun Varsa):
```batch
.\gradlew clean
.\gradlew assembleRelease
```

## 🔍 APK Doğrulama

APK'nın doğru oluşturulduğunu kontrol edin:
```batch
# APK boyutu (yaklaşık 15-30 MB olmalı)
dir app\build\outputs\apk\release\app-release.apk

# APK içeriğini kontrol et (opsiyonel)
.\gradlew assembleRelease --info
```

## 📱 Telefon Gereksinimleri

### Minimum:
- Android 8.0 (API 26) veya üzeri
- 2 GB RAM
- 100 MB boş alan

### Önerilen:
- Android 11 (API 30) veya üzeri
- 4 GB RAM
- Bluetooth 4.0 veya üzeri
- GNSS/GPS desteği

## ⚠️ Önemli Notlar

### Bluetooth Bağlantısı:
1. Uygulamayı ilk açtığınızda Bluetooth izinlerini verin
2. GNSS cihazınızı önce telefon ayarlarından eşleştirin
3. Sonra uygulamadan bağlantı kurun

### GNSS Cihaz Seçimi:
1. **Cihaz** > **Gezici** menüsüne gidin
2. GNSS marka ve modelinizi seçin
3. Ayarları yapılandırın ve kaydedin

### NCN Dosya İşlemleri:
1. NCN dosyaları telefon hafızasında olmalı
2. **Proje** > **Nokta Veri Tabanı** > **İçe Aktar** > **NCN Dosyası**
3. Dışa aktarma otomatik olarak dosyayı kaydeder

## 🐛 Hata Ayıklama

### Logcat Görüntüleme (Sorun varsa):
```batch
# Android Studio'da
View > Tool Windows > Logcat

# Veya terminalden
adb logcat | findstr "tugis3"
```

### Uygulama Verilerini Temizleme:
Telefonda:
1. **Ayarlar** > **Uygulamalar** > **TUGIS3**
2. **Depolama** > **Verileri Temizle**
3. Uygulamayı yeniden başlatın

## 📞 Destek

Sorun yaşarsanız:
1. Build log'larını kontrol edin
2. YENI_OZELLIKLER.md dosyasını okuyun
3. AndroidManifest.xml'de duplicate tanım olmadığından emin olun

---
**Son Güncelleme:** 2025-01-11

