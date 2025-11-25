# 📋 SONRAKI ADIMLAR - Build Sonrası

## ⚠️ ÖNCELİKLE YAPILACAK

### Build Hatası Varsa
```cmd
complete_build_fix.bat
```

Bu script:
- ✅ Duplicate dosyaları temizler
- ✅ KSP cache'i siler
- ✅ Otomatik build yapar
- ✅ APK konumunu gösterir

---

## 1️⃣ BUILD BAŞARILI OLUNCA

### APK'yı Bul
```
app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
```

### Telefona Yükle
- Galaxy A21s için: **app-arm64-v8a-debug.apk** ✅

## 2️⃣ TEST EDİLECEKLER

### ✅ Bluetooth Bağlantısı
- [ ] Bluetooth menüsü açılıyor mu?
- [ ] GNSS cihazı (South X1) bulunuyor mu?
- [ ] Eşleşme isteği geliyor mu?
- [ ] Bağlantı yapılıyor mu?
- [ ] "GPS Bağlandı" mesajı görünüyor mu?

### ✅ GPS Sinyalleri
- [ ] Uydu sayısı gösteriliyor mu? (örn: 12/20)
- [ ] HDOP/PDOP değerleri görünüyor mu?
- [ ] Konum bilgisi (Enlem/Boylam) geliyor mu?
- [ ] Gerçek zamanlı güncellenme oluyor mu?

### ✅ Cihaz İletişimi
- [ ] "Tara" butonu çalışıyor mu?
- [ ] COM portları listeleniyor mu?
- [ ] Bluetooth seri port seçilebiliyor mu?
- [ ] Bağlantı durumu gösteriliyor mu?

### ✅ Nokta İçe Aktarma
- [ ] .ncn dosyası seçilebiliyor mu?
- [ ] Formatı doğru okuyor mu?
- [ ] Noktalar listeye ekleniyor mu?
- [ ] Haritada gösteriliyor mu?

## 3️⃣ SORUN VARSA

### Bluetooth Bulamıyorsa
```
Cihaz → Bluetooth Ayarları → Tara
```
- İzinleri kontrol et (Konum, Bluetooth)
- Cihazın Bluetooth açık olsun
- Cihaz keşfedilebilir modda olsun

### GPS Sinyal Gelmiyorsa
```
Cihaz İletişimi → Tara → Bluetooth Seri Port Seç
```
- Bağlantı türü: Bluetooth
- Baud rate: 115200 (veya cihaz ayarı)
- NMEA protokolü aktif olsun

### Yükleme Uzun Sürüyorsa
- Eski APK'yı kaldır: `adb uninstall com.example.tugis3`
- Telefonu yeniden başlat
- USB hata ayıklama kapalı/açık yap

## 4️⃣ PERFORMANS İYİLEŞTİRME

### APK Boyutunu Küçült
```cmd
gradlew assembleRelease --no-daemon
```

### Yükleme Hızlandır
```cmd
gradlew installDebug --no-daemon
```

## 5️⃣ YENİ ÖZELLİKLER EKLENDİ

✅ **NCN Format Desteği**
- Nokta içe aktarma: .ncn, .csv, .txt
- Format: `NOKTA_ADI DOĞU KUZEY YÜKSEKLİK`

✅ **Bluetooth İyileştirmeleri**
- Otomatik tarama
- Eşleşme kontrolü
- Bağlantı durumu göstergesi
- GPS veri akışı

✅ **Dosya Yönetimi**
- Telefon hafızası erişimi
- Yandex Disk yerine lokal depolama
- Dosya seçici düzeltildi

## 6️⃣ BUILD HATALARI

### KSP Hatası
```cmd
fix_ksp_complete.bat
```

### Gradle Cache
```cmd
clean_ksp_caches.bat
```

### Nuclear Clean
```cmd
nuclear_clean.bat
```

## 7️⃣ DESTEK

### Logları Göster
```cmd
gradlew assembleDebug --info > build_log.txt
```

### Cihaz Logları
```cmd
adb logcat -s TuGIS3
```

---
**Hazırlayan:** GitHub Copilot  
**Tarih:** 2025-01-03  
**Proje:** TuGIS3 - Jeodezi ve Harita Mühendisliği Uygulaması

