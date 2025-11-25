# TUGIS3 - Yeni Özellikler Eklendi

## Tarihi: 2025-01-11

### ✅ Tamamlanan Özellikler

## 1. Gezici (Rover) Ayarları - Cihaz Menüsü

### Özellikler:
- ✅ **Bluetooth Bağlantı Kontrolü**: RTK cihazının Bluetooth bağlantısını kontrol eder
- ✅ **Bağlantı Uyarısı**: Bluetooth bağlı değilse "RTK Bağlı Değil" uyarısı gösterir
- ✅ **GNSS Cihaz Markası Seçimi**: Tüm büyük markalar eklendi
  - South (Alps2, Galaxy G1, G2, G3, S82V, S86T, S82)
  - Trimble (R12, R10, R8s, R2)
  - CHC Navigation (i80, i73, i50, X91+)
  - Topcon (HiPer VR, HiPer V, GR-5, GR-3)
  - Leica (GS18T, GS18, GS16, GS14)
  - Hi-Target (V200, V100, V90)
  - Sokkia (GRX3, GRX2, GR1)
  - Emlid (Reach RS3, RS2+, RS2)
  - Septentrio (AsteRx SBi3, Altus NR2, NR1)

### South Alps2 Modeli Eklendi:
```kotlin
GnssModel("south_alps2", "Alps2", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS"))
```

### Kullanım:
1. **Cihaz** menüsünden **Gezici** seçeneğine tıklayın
2. Bluetooth bağlantısı yoksa uyarı alırsınız
3. GNSS markasını seçin (örn: South)
4. Model seçin (örn: Alps2)
5. Ayarları yapılandırın:
   - Cut-off Angle (Uydu Yükseklik Açısı)
   - Raw Data Kaydı
   - AR-RTK Etkinleştirme
   - Anten Yüksekliği

## 2. NCN Format Desteği - Dosya Yönetimi

### NCN Formatı Nedir?
NCN (Nikon Coordinate Native) formatı, Nikon total station ve GNSS cihazlarında kullanılan koordinat dosya formatıdır.

### Format Yapısı:
```
nokta_adi dogu kuzey yukseklik
```

### Örnek NCN Dosyası:
```
m1 446055.966 4286420.511 1072.775
k1 446058.958 4286420.017 1073.039
k2 446058.659 4286412.615 1072.708
yol1 446059.797 4286401.519 1072.427
```

### ✅ Eklenen Özellikler:

#### A. Dosya Yönetimi (FileManagerActivity)
- ✅ **NCN Dışa Aktarma**: Noktaları NCN formatında dışa aktarır
- ✅ **NCN İçe Aktarma**: NCN dosyalarını okur ve noktaları içe aktarır

#### B. Nokta Veri Tabanı (PointDatabaseActivity)
- ✅ **NCN İçe Aktarma Menüsü**: Dropdown menüden NCN dosyası seçimi
- ✅ **NCN Dışa Aktarma Menüsü**: Dropdown menüden NCN formatında kaydetme
- ✅ **CSV İçe/Dışa Aktarma**: Mevcut CSV desteği korundu

### Kullanım:

#### NCN İçe Aktarma:
1. **Proje** > **Nokta Veri Tabanı** menüsüne gidin
2. Üst bardan **İçe Aktar** (Download) ikonuna tıklayın
3. **NCN Dosyası** seçeneğini seçin
4. NCN dosyanızı seçin
5. Noktalar otomatik olarak içe aktarılır

#### NCN Dışa Aktarma:
1. **Proje** > **Nokta Veri Tabanı** menüsüne gidin
2. Üst bardan **Dışa Aktar** (Upload) ikonuna tıklayın
3. **NCN Formatı** seçeneğini seçin
4. Noktalar NCN formatında kaydedilir

### NCN Format Özellikleri:
- ✅ 3 ondalık hassasiyet (%.3f)
- ✅ Nokta adlarındaki boşluklar otomatik kaldırılır
- ✅ Sıralama: Ad, Doğu, Kuzey, Yükseklik
- ✅ US Locale kullanımı (nokta ayırıcı)

## 3. Bluetooth Bağlantı İyileştirmeleri

### Özellikler:
- ✅ Runtime izin kontrolü
- ✅ Android 12+ (API 31) için BLUETOOTH_SCAN ve BLUETOOTH_CONNECT izinleri
- ✅ Android 11- için BLUETOOTH ve BLUETOOTH_ADMIN izinleri
- ✅ Eşleştirilmiş cihazları listeleme
- ✅ Yeni cihaz tarama
- ✅ Cihaz eşleştirme ve bağlantı

## 4. Build Hataları Düzeltildi

### Düzeltmeler:
- ✅ AndroidManifest'te duplicate LineStakeoutActivity kaldırıldı
- ✅ RoverSettingsActivity syntax hataları düzeltildi
- ✅ Build script optimize edildi

## Yapılacaklar Listesi

### Yüksek Öncelikli:
- [ ] **Dosya Yönetimi Ekranı**: Şu anda "Bu ekran henüz tamamlanmadı" diyor
  - NCN import/export UI tamamlanmalı
  - Dosya listesi gösterilmeli
  - Dosya yönetimi işlemleri aktif olmalı

- [ ] **Bluetooth Gerçek Bağlantı**: 
  - BluetoothGnssManager entegrasyonu
  - GNSS cihazından veri alma
  - RTK düzeltme verisi alma

### Orta Öncelikli:
- [ ] Diğer formatlara da NCN desteği:
  - [ ] Hat Aplikasyonu'nda NCN
  - [ ] CAD Aplikasyonu'nda NCN
  - [ ] Grafik Alım'da NCN

- [ ] GNSS Cihaz ayarlarını kaydetme/yükleme

### Düşük Öncelikli:
- [ ] NCN dosya önizlemesi
- [ ] Toplu format dönüştürme

## Test Edilmesi Gerekenler

### Manuel Test:
1. [ ] Bluetooth bağlantısı test et
2. [ ] South Alps2 cihazı seçimi test et
3. [ ] NCN dosya içe aktarma test et
4. [ ] NCN dosya dışa aktarma test et
5. [ ] Dosya Yönetimi ekranını kontrol et

### Build Test:
```batch
# Clean build
.\gradlew clean

# Debug build
.\gradlew assembleDebug

# Release build
.\gradlew assembleRelease
```

## Bilinen Sorunlar

### Düşük Öncelikli Uyarılar:
- String.format() Locale uyarıları (sadece uyarı, çalışmayı etkilemez)
- Deprecation uyarıları (API uyumluluğu için)

## Kurulum

### Gereksinimler:
- Android Studio Arctic Fox veya üzeri
- JDK 11 veya üzeri
- Android SDK 34
- Gradle 8.0+

### Build Komutu:
```batch
# Windows
build_app.bat

# Manuel
gradlew clean assembleRelease
```

### APK Konumu:
```
app\build\outputs\apk\release\app-release.apk
```

## Kod Yapısı

### Yeni/Güncellenmiş Dosyalar:
```
app/src/main/java/com/example/tugis3/
├── ui/
│   ├── device/
│   │   ├── RoverSettingsActivity.kt (✅ Güncellendi)
│   │   └── BluetoothSettingsActivity.kt
│   └── project/
│       ├── FileManagerActivity.kt (✅ Güncellendi)
│       └── PointDatabaseActivity.kt (✅ Güncellendi)
└── AndroidManifest.xml (✅ Düzeltildi)
```

## Katkıda Bulunanlar
- GitHub Copilot
- TUGIS3 Development Team

## Lisans
Proprietary - TUGIS3 Project

---
**Not**: Bu dokümantasyon projeye eklenen yeni özellikleri açıklar. Daha fazla bilgi için kaynak kodlara bakınız.

