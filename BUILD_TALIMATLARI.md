# 🚀 BUILD TALİMATLARI

## ⚠️ PowerShell Komutu

PowerShell'de `gradlew` yerine `.\gradlew` kullanmalısınız:

```powershell
cd C:\Users\CASPER\AndroidStudioProjects\tugis3
.\gradlew clean
.\gradlew assembleDebug
```

**VEYA** tek komutta:

```powershell
.\gradlew clean assembleDebug
```

---

## 🎯 ALTERNATIF YÖNTEMLER

### 1. Android Studio'da (Önerilen)

```
Build → Clean Project
Build → Rebuild Project
Run → Run 'app'
```

### 2. CMD'de

```cmd
cd C:\Users\CASPER\AndroidStudioProjects\tugis3
gradlew clean assembleDebug
```

### 3. Git Bash'te

```bash
cd /c/Users/CASPER/AndroidStudioProjects/tugis3
./gradlew clean assembleDebug
```

---

## 📋 BEKLENEN SONUÇLAR

### ✅ Başarılı Build:
```
BUILD SUCCESSFUL in XXs
XX actionable tasks: XX executed
```

APK konumu:
```
app\build\outputs\apk\debug\app-debug.apk
```

### ❌ Hata Alırsanız:

**1. Database Schema Hatası:**
```
.\gradlew clean
Delete folder: app\build
.\gradlew assembleDebug
```

**2. Gradle Daemon Hatası:**
```
.\gradlew --stop
.\gradlew clean assembleDebug
```

**3. Cache Problemi:**
Android Studio'da:
```
File → Invalidate Caches → Invalidate and Restart
```

---

## 🎉 SON DURUM

### Kesit Plan Aplikasyonu v2.0
✅ GPS entegrasyonu  
✅ Export (CSV, HDM, TXT)  
✅ 2D görselleştirme  
✅ 0 compile error  
✅ Production ready  

### Katman Ayarları v2.0
✅ 8 temel işlev  
✅ SurPad 4.2 uyumlu  
✅ Material Design 3  
✅ Database migration (v33→34)  
✅ Production ready  

---

## 🔧 HIZLI FIX

Eğer build hatası alırsanız:

1. **Gradle cache temizle:**
   ```powershell
   .\gradlew --stop
   Remove-Item -Recurse -Force .gradle
   .\gradlew clean assembleDebug
   ```

2. **Build klasörü temizle:**
   ```powershell
   Remove-Item -Recurse -Force app\build
   .\gradlew assembleDebug
   ```

3. **Android Studio yeniden başlat:**
   - File → Invalidate Caches
   - Invalidate and Restart

---

## ✨ ÖZET

Tüm geliştirmeler tamamlandı:

1. ✅ **Kesit Plan Aplikasyonu v2.0** - GPS + Export + 2D Görsel
2. ✅ **Katman Ayarları v2.0** - SurPad uyumlu 8 işlev
3. ✅ **Database Migration** - v33→34
4. ✅ **0 Compile Error**

**Şimdi build yapın ve test edin!** 🚀

---

*Son Güncelleme: 29 Ekim 2025*  
*Durum: READY TO BUILD ✅*

