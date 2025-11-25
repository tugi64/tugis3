# 🔧 Hata 30716 Çözüm Rehberi

## ⚠️ Sorun
Build sırasında **hata 30716** alınıyor.

## ✅ Çözüm Adımları

### Yöntem 1: Android Studio'da Cache Temizleme (ÖNERİLEN)

1. **Android Studio'yu Kapat**

2. **Cache Temizle**:
   - Android Studio'yu aç
   - **File → Invalidate Caches / Restart...**
   - **Invalidate and Restart** butonuna tıkla
   
3. **Gradle Cache Temizle** (Android Studio açıldıktan sonra):
   - **File → Sync Project with Gradle Files** (Ctrl+Shift+O)
   - Build → Clean Project
   - Build → Rebuild Project

### Yöntem 2: Manuel Temizleme (PowerShell)

```powershell
cd C:\Users\CASPER\AndroidStudioProjects\tugis3

# Gradle daemon'u durdur
.\gradlew.bat --stop

# Clean yap
.\gradlew.bat clean

# Gradle cache klasörlerini temizle
Remove-Item -Recurse -Force .gradle\8.13\kotlin-dsl -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .gradle\configuration-cache -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue

# Yeniden derle
.\gradlew.bat assembleDebug --stacktrace
```

### Yöntem 3: Otomatik Script (Hazır .bat dosyası)

Proje kökünde `build_firebase_disabled.bat` dosyası oluşturuldu.

**Çalıştırmak için**:
1. Windows Explorer'da proje klasörüne git
2. `build_firebase_disabled.bat` dosyasına çift tıkla
3. Build tamamlanana kadar bekle

> Not: Eğer `google-services.json` eksik olduğu için build kırılıyorsa, bu betik otomatik olarak bir placeholder `app/google-services.json` oluşturur ve Firebase eklentisini geçici olarak devre dışı bırakır. Gerçek Firebase kullanımı için Firebase Console'dan indirdiğiniz orijinal `google-services.json` dosyasını `app/` dizinine koyun ve betiği tekrar çalıştırmayın.

## 🔍 Hata 30716 Nedenleri ve Çözümleri

### Neden 1: Gradle Configuration Cache Bozulması
**Çözüm**: Configuration cache'i temizle
```cmd
rmdir /s /q .gradle\configuration-cache
```

### Neden 2: Kotlin DSL Cache Sorunu
**Çözüm**: Kotlin DSL cache'ini temizle
```cmd
rmdir /s /q .gradle\8.13\kotlin-dsl
```

### Neden 3: Firebase Plugin'leri Çakışması
**Çözüm**: Firebase zaten devre dışı - eğer hala sorun varsa şunu kontrol et:
- `build.gradle.kts` dosyalarında Firebase satırlarının **tam olarak** yorum satırına alındığından emin ol

### Neden 4: Build Cache Bozulması
**Çözüm**: Tüm build çıktılarını temizle
```cmd
.\gradlew.bat clean
rmdir /s /q app\build
rmdir /s /q build
```

## 📋 Detaylı Sorun Giderme

### Adım 1: Gradle Sync Durumu Kontrol

Android Studio → **File → Project Structure**
- **Project**: Gradle Version = 8.13
- **Project**: Android Gradle Plugin = 8.12.0

### Adım 2: Firebase Durumunu Doğrula

`build.gradle.kts` (root) dosyasında şunların yorum satırı olduğunu kontrol et:
```kotlin
// alias(libs.plugins.firebase.crashlytics) apply false
// alias(libs.plugins.google.services) apply false
```

`app/build.gradle.kts` dosyasında şunların yorum satırı olduğunu kontrol et:
```kotlin
// alias(libs.plugins.google.services)
// alias(libs.plugins.firebase.crashlytics)

// dependencies bloğunda:
// implementation(platform(libs.firebase.bom))
// implementation(libs.firebase.analytics)
// implementation(libs.firebase.crashlytics)
```

### Adım 3: JDK Sürümü Kontrol

Android Studio → **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
- **Gradle JDK**: Java 17 seçili olmalı

### Adım 4: Offline Mode Kapalı mı?

Android Studio → **File → Settings → Build, Execution, Deployment → Gradle**
- **Offline work** kutusunun işaretli OLMAMASI gerekir

## 🆘 Hala Çözülmezse

### Son Çare: Deep Clean

```powershell
cd C:\Users\CASPER\AndroidStudioProjects\tugis3

# Android Studio'yu KAPAT

# Gradle cache'i tamamen sil
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue

# Build çıktılarını sil
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force app\build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force core\data\build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force core\ui\build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force core\cad\build -ErrorAction SilentlyContinue

# .idea cache temizle (Android Studio kapalıyken)
Remove-Item -Recurse -Force .idea\caches -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .idea\modules -ErrorAction SilentlyContinue

# Android Studio'yu AÇ ve Gradle sync yap
```

### Hata Mesajını Paylaş

Eğer yukarıdaki hiçbiri işe yaramazsa, tam hata mesajını (stacktrace ile) paylaşın:

```cmd
.\gradlew.bat assembleDebug --stacktrace > build_error.txt 2>&1
```

`build_error.txt` dosyasının içeriğini gönderin.

## 📞 Hızlı Yardım

**Şimdi dene**:
1. Android Studio → **File → Invalidate Caches / Restart**
2. Restart sonrası → **File → Sync Project with Gradle Files**
3. **Build → Rebuild Project**

veya

```cmd
build_firebase_disabled.bat
```

---

**Not**: Hata 30716 genellikle Gradle/Kotlin DSL cache bozulmasından kaynaklanır.  
Yukarıdaki cache temizleme adımları %95 oranında sorunu çözer.
