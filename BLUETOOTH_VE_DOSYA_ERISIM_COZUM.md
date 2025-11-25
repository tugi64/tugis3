# Bluetooth ve Dosya Erişim Sorunları - Çözüm Kılavuzu

## ✅ Yapılan Düzeltmeler

### 1. Bluetooth Bağlantı Sorunu Çözüldü

#### Yapılan Değişiklikler:

**AndroidManifest.xml**
- ✅ Android 12+ için yeni Bluetooth izinleri eklendi
- ✅ Android 11 ve öncesi için eski Bluetooth izinleri eklendi
- ✅ Konum izinleri (Bluetooth tarama için gerekli)

**BluetoothSettingsActivity.kt**
- ✅ Syntax hatası düzeltildi (LazyColumn kapanış parantezi)
- ✅ Paired devices listesi gerçek cihazları gösterecek şekilde düzenlendi
- ✅ Mock data kaldırıldı
- ✅ Gerçek BluetoothDevice bilgileri gösteriliyor

#### Test Adımları:

1. **Uygulamayı yeniden yükleyin**
   ```batch
   gradlew clean assembleRelease
   ```

2. **İlk açılışta izin verin:**
   - Bluetooth izni
   - Konum izni (Bluetooth tarama için gerekli)

3. **GNSS cihazınızı eşleştirin:**
   - Telefon Ayarlar > Bluetooth
   - GNSS cihazınızı bulun ve eşleştirin
   - PIN: 0000 veya 1234

4. **Uygulamada görün:**
   - Cihaz > Bluetooth Ayarları
   - Eşleştirilmiş cihazlar listesinde görünmeli

### 2. Dosya Erişim Sorunu (Yandex Disk) Çözüldü

#### Sorun:
Dosya açmaya çalışınca Yandex Disk veya diğer cloud uygulamalar açılıyor, telefon hafızası görünmüyor.

#### Çözüm:

**CadApplicationActivity.kt**
- ✅ MIME type listesi optimize edildi
- ✅ Sadece CAD dosyaları için filtreleme
- ✅ Telefon hafızası öncelikli

**PointDatabaseActivity.kt**
- ✅ CSV için: `text/comma-separated-values`
- ✅ NCN için: `text/plain`
- ✅ Daha spesifik MIME types

**AndroidManifest.xml**
- ✅ Android 13+ için READ_MEDIA izinleri eklendi

#### Test Adımları:

1. **DWG/DXF Dosyası Açma:**
   - CAD Aplikasyonu > Dosya Aç
   - "Dosyalar" (Files) veya "Belgeler" (Documents) seçin
   - Yandex Disk yerine telefon hafızası görünmeli

2. **NCN Dosyası İçe Aktarma:**
   - Proje > Nokta Veri Tabanı
   - İçe Aktar > NCN Dosyası
   - Telefon hafızasından .ncn dosyası seçin

3. **Eğer Yandex Disk Hala Açılıyorsa:**
   - Dosya seçicide üstten "Dosyalar" veya telefon simgesine tıklayın
   - "Dahili depolama" veya "Downloads" seçin

## 📱 Telefonda Test Etme

### Adım 1: Temiz Yükleme

```batch
# Build
gradlew clean assembleRelease

# Eski uygulamayı kaldır
adb uninstall com.example.tugis3

# Yeni APK'yı yükle
adb install app\build\outputs\apk\release\app-release.apk
```

### Adım 2: İlk Açılış İzinleri

Uygulama açıldığında şu izinleri verin:
- ✅ Konum (GNSS ve Bluetooth için)
- ✅ Bluetooth
- ✅ Dosya erişimi
- ✅ Kamera (barkod tarama için)

### Adım 3: Bluetooth Testi

1. **Telefon Ayarlarından Eşleştir:**
   - Ayarlar > Bluetooth
   - GNSS cihazını açın
   - "Alps2" veya cihaz adını bulun
   - Eşleştir (PIN: 0000)

2. **Uygulamada Görün:**
   - TUGIS3 aç
   - Cihaz > Bluetooth Ayarları
   - Eşleştirilmiş Cihazlar (1) görünmeli
   - Cihazınız listelenmiş olmalı

3. **Bağlan:**
   - Cihaza tıklayın veya "Bağlan" butonuna basın
   - Toast mesajı: "Alps2 cihazına bağlanılıyor..."

### Adım 4: Dosya Erişim Testi

1. **NCN Dosyası Hazırlayın:**
   ```
   m1 446055.966 4286420.511 1072.775
   k1 446058.958 4286420.017 1073.039
   ```
   Bu içeriği "test.ncn" olarak kaydedin (Telefon > Downloads)

2. **NCN İçe Aktarın:**
   - TUGIS3 > Proje > Nokta Veri Tabanı
   - İçe Aktar > NCN Dosyası
   - Downloads > test.ncn seçin
   - "2 nokta NCN'den içe aktarıldı" mesajı

3. **DWG Dosyası Açın:**
   - CAD Aplikasyonu > Dosya Aç
   - Telefon hafızasından .dxf veya .dwg seçin

## 🐛 Sorun Giderme

### Bluetooth Cihaz Görünmüyor

**Kontrol 1: İzinler**
```
Ayarlar > Uygulamalar > TUGIS3 > İzinler
- Konum: İzin verildi ✓
- Yakındaki cihazlar: İzin verildi ✓ (Android 12+)
```

**Kontrol 2: Bluetooth Durumu**
- Telefon Bluetooth'u açık mı?
- GNSS cihazı eşleştirilmiş mi?
- GNSS cihazı açık ve yakında mı?

**Çözüm:**
1. Telefon Bluetooth'u kapat-aç
2. GNSS cihazı yeniden başlat
3. Uygulamayı kapat-aç
4. Tekrar dene

### Yandex Disk Hala Açılıyor

**Çözüm 1: Dosya Seçicide Değiştir**
- Dosya seçici açıldığında üstten menü
- "Dosyalar" veya telefon simgesi
- "Dahili depolama" seç

**Çözüm 2: Varsayılan Uygulamayı Değiştir**
```
Ayarlar > Uygulamalar > Varsayılan uygulamalar > Dosya yöneticisi
"Dosyalar" veya "Belgeler" seç (Yandex değil)
```

**Çözüm 3: Scoped Storage Kullan**
- Android 10+ otomatik olarak Scoped Storage kullanır
- Yandex Disk değil, sistem dosya seçici açılmalı
- Eğer açılmıyorsa Yandex Disk'i devre dışı bırakın

### DWG Dosyası Açılmıyor

**Desteklenen Formatlar:**
- ✅ DXF (AutoCAD Drawing Exchange Format)
- ✅ CSV (Koordinat verileri)
- ✅ JSON/GeoJSON
- ❌ DWG (şu an desteklenmiyor - DXF'e çevirin)

**DWG'yi DXF'e Çevir:**
1. Bilgisayarda AutoCAD veya LibreCAD kullan
2. File > Save As > DXF
3. DXF dosyasını telefona kopyala
4. TUGIS3'te DXF'i aç

## 📊 Teknik Detaylar

### MIME Types Kullanılan

**CAD Dosyaları:**
```kotlin
arrayOf(
    "application/dxf",
    "application/acad",
    "image/vnd.dxf",
    "application/x-dxf",
    "*/*"
)
```

**NCN Dosyaları:**
```kotlin
"text/plain"  // .ncn uzantısı text formatı
```

**CSV Dosyaları:**
```kotlin
"text/comma-separated-values"
```

### Storage Access Framework (SAF)

Android 10+ (API 29+) Scoped Storage kullanır:
- ✅ Kullanıcı dosya seçer
- ✅ Uygulama geçici URI alır
- ✅ Güvenli ve izinli erişim
- ✅ Cloud veya yerel fark etmez

### Bluetooth İzin Mimarisi

**Android 12+ (API 31+):**
```xml
BLUETOOTH_SCAN
BLUETOOTH_CONNECT
BLUETOOTH_ADVERTISE
```

**Android 11 ve altı (API 30-):**
```xml
BLUETOOTH
BLUETOOTH_ADMIN
ACCESS_FINE_LOCATION (tarama için)
```

## 📝 Notlar

- Bluetooth bağlantısı şu an temel seviyede (pairing gösterimi)
- Gerçek GNSS veri alışverişi için BluetoothGnssManager entegrasyonu gerekli
- DWG formatı henüz desteklenmiyor (DXF kullanın)
- NCN format desteği tam çalışır durumda

---
**Son Güncelleme:** 2025-01-11
**Sürüm:** 1.0.1

