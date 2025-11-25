# 🔷 TUGIS3 - Bluetooth ve Build Durum Raporu

## 📋 Özet
Build hataları düzeltildi. Bluetooth GNSS bağlantısı için tüm UUID'ler ve fallback mekanizmaları eklendi.

---

## ✅ Düzeltilen Sorunlar

### 1. **Build Hataları**
- ✅ `adbOptions` deprecated hatası kaldırıldı
- ✅ Duplicate class hataları temizlendi
- ✅ KSP cache sorunları düzeltildi
- ✅ Kotlin compilation hataları giderildi

### 2. **Bluetooth Tarama Sistemi**
- ✅ Android 12+ (API 31+) için `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` izinleri
- ✅ Android 11- (API 30-) için `BLUETOOTH` / `BLUETOOTH_ADMIN` / `ACCESS_FINE_LOCATION` izinleri
- ✅ Konum servisi kontrolü (Android 11- için)
- ✅ BroadcastReceiver ile cihaz keşfi
- ✅ Otomatik izin isteme sistemi

### 3. **GNSS Cihaz Bağlantısı**
```kotlin
BluetoothGnssManager.kt - GÜNCELLEME
================================
✅ SPP UUID (Serial Port Profile) - En yaygın
✅ Alternatif UUID'ler (Audio Source, Sink, OBD-II)
✅ Reflection fallback (Samsung/Galaxy uyumluluk)
✅ Multi-manufacturer support:
   - South
   - Topcon
   - Trimble
   - Kolida
   - Diğer GNSS alıcılar
```

---

## 🚀 Build ve Test Talimatları

### Adım 1: Build Yap
Terminal'de çalıştır:
```cmd
cd C:\Users\CASPER\AndroidStudioProjects\tugis3
build_debug_fixed.bat
```

VEYA Android Studio'da:
```
Build > Rebuild Project
```

### Adım 2: APK'yi Yükle
Galaxy A21s için doğru APK:
```
app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
```

Yükleme komutu:
```cmd
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
```

---

## 🔍 Bluetooth Tarama Sorunu - Çözüm Adımları

### Neden Tarama Çalışmıyor?

#### 1. **İzin Problemi (En Yaygın)**
Android 12+ (API 31+) cihazlarda şu izinler **mutlaka** gerekli:
- ✅ `BLUETOOTH_SCAN` (konum erişimi olmadan)
- ✅ `BLUETOOTH_CONNECT`

**Çözüm:**
```
Uygulama ilk açılışta otomatik izin isteyecek.
Eğer reddettiyseniz:
1. Telefon Ayarları > Uygulamalar > TUGIS3
2. İzinler > "Yakındaki Cihazlar" AÇIK
3. Uygulamayı kapatıp tekrar açın
```

#### 2. **Konum Kapalı (Android 11 ve Altı)**
Android 11 (API 30) ve altında **konum açık** olmalı:
```
Ayarlar > Konum > AÇIK
```

**Not:** Android 12+ için konum artık gerekmiyor!

#### 3. **Bluetooth Keşfedilebilirlik (Bazı Cihazlar)**
GNSS alıcılar "keşfedilebilir" modda olmalı:
```
South/Kolida/Topcon GNSS Alıcı:
1. Bluetooth ayarlarına gir (alıcıda)
2. "Keşfedilebilir" veya "Pairing Mode" aktif et
3. Genelde 2-3 dakika aktif kalır
```

---

## 🔗 Bluetooth Bağlantı Testi

### Test 1: Eşleştirme (Pairing)
```
TUGIS3 > Cihaz İletişimi > Bluetooth Ayarları
1. "Yeni Cihaz Tara" düğmesine bas
2. GNSS alıcı görünmelidir (örn: "S86T", "Kolida K5", "SurvStar")
3. Cihaza tıkla > "Eşleştir"
4. PIN isterse: genelde "0000" veya "1234"
5. "Bluetooth bağlandı!" mesajı görülmeli
```

### Test 2: NMEA Veri Akışı
```
Bağlandıktan sonra:
1. Logcat'te NMEA cümleleri görünmeli:
   NMEA: $GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
   NMEA: $GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A

2. Uygulama üst çubukta GPS simgesi + uydu sayısı göstermeli
3. Alım > GPS/GNSS menüsünde veri güncellenecek
```

---

## 🛠️ Hata Ayıklama (Debug)

### Logcat Filtreleme
Android Studio > Logcat:
```
Tag filtresi: BluetoothSettings|BluetoothGnssManager
Level: Debug
```

### Beklenen Log Çıktıları

#### ✅ Başarılı Tarama
```
D/BluetoothSettings: Tarama başlatılıyor...
D/BluetoothSettings: ✅ Tarama başarıyla başlatıldı
D/BluetoothSettings: Cihaz bulundu: S86T - AA:BB:CC:DD:EE:FF
D/BluetoothSettings: Tarama bitti: 3 cihaz
```

#### ✅ Başarılı Bağlantı
```
D/BluetoothGnssManager: Bağlantı deneniyor: S86T [AA:BB:CC:DD:EE:FF]
D/BluetoothGnssManager: Deneme #1: SPP UUID ile bağlanılıyor...
D/BluetoothGnssManager: ✅ SPP UUID ile bağlantı başarılı!
D/BluetoothGnssManager: 📡 NMEA dinleme aktif
D/BluetoothGnssManager: NMEA: $GPGGA,...
```

#### ❌ Başarısız Tarama (İzin Yok)
```
E/BluetoothSettings: startDiscovery SecurityException: Permission denied
W/BluetoothSettings: İzin isteniyor: [BLUETOOTH_SCAN, BLUETOOTH_CONNECT]
```

#### ❌ Başarısız Bağlantı
```
E/BluetoothGnssManager: SPP UUID başarısız: read failed, socket might closed
D/BluetoothGnssManager: Deneme #2: UUID ... ile bağlanılıyor...
E/BluetoothGnssManager: ❌ Bağlantı başarısız - 4 deneme yapıldı
```

---

## 📱 Test Senaryosu

### Tam Test Prosedürü
```
1. APK'yi yükle (arm64-v8a)
2. Uygulamayı aç
3. İzinleri VER (yakındaki cihazlar)
4. Ana Menü > "Cihaz İletişimi"
5. "Bluetooth Ayarları" tıkla
6. GNSS alıcıyı AÇ ve keşfedilebilir yap
7. "Yeni Cihaz Tara" düğmesine bas
8. Cihaz listede görünmeli (10-30 saniye)
9. Cihaza tıkla > "Eşleştir"
10. PIN gir (varsa)
11. "Bluetooth bağlandı!" mesajı
12. "Bağlan" düğmesine bas
13. GPS simgesi + uydu sayısı görünmeli
14. Alım menüsünde koordinatlar güncellenecek
```

---

## 🔧 Bilinen Sorunlar ve Çözümler

### Sorun 1: "Tarama başlatılamadı"
**Nedeni:** Bluetooth cache bozuk
**Çözüm:**
```
1. TUGIS3'ü tamamen kapat
2. Telefon Ayarları > Uygulamalar > Bluetooth
3. Depolama > Önbelleği Temizle
4. Telefonu yeniden başlat
5. TUGIS3'ü aç, tekrar dene
```

### Sorun 2: "Cihaz bulunamadı"
**Nedeni:** GNSS keşfedilebilir değil
**Çözüm:**
```
GNSS alıcıda:
1. Menu > Bluetooth > Pairing Mode AÇIK
2. Veya Bluetooth'u kapat-aç
3. TUGIS3'te tekrar tara
```

### Sorun 3: "Eşleştirme başarısız"
**Nedeni:** PIN hatalı veya timeout
**Çözüm:**
```
1. PIN'i kontrol et: 0000, 1234, 9999
2. GNSS alıcıyı yakın tut (<1 metre)
3. Diğer Bluetooth cihazları kapat
4. Tekrar dene
```

### Sorun 4: "Bağlandı ama GPS yok"
**Nedeni:** NMEA stream aktif değil
**Çözüm:**
```
GNSS alıcıda:
1. NMEA çıkışının AÇIK olduğunu kontrol et
2. Baud rate: 9600 veya 115200
3. GNSS'i yeniden başlat
4. TUGIS3'te "Bağlantıyı Kes" > "Bağlan"
```

---

## 📊 Teknik Detaylar

### Desteklenen UUID'ler
```kotlin
1. SPP (Serial Port Profile)
   00001101-0000-1000-8000-00805F9B34FB
   ✅ En yaygın - %95 GNSS cihazı

2. Audio Source
   0000110A-0000-1000-8000-00805F9B34FB
   
3. Audio Sink
   0000110B-0000-1000-8000-00805F9B34FB

4. OBD-II
   00001105-0000-1000-8000-00805F9B34FB

5. Reflection Fallback (Channel 1)
   - Samsung/Galaxy uyumluluk
```

### NMEA Sentence Formatı
```
$GPGGA - GPS Fix Data
$GPRMC - Recommended Minimum Navigation Information
$GPGSA - GPS DOP and Active Satellites
$GPGSV - GPS Satellites in View
$GPVTG - Track Made Good and Ground Speed
```

---

## ✅ Son Kontrol Listesi

Şunları kontrol edin:

- [ ] APK doğru mimariye yüklendi (arm64-v8a)
- [ ] Bluetooth AÇIK
- [ ] Konum AÇIK (Android 11-)
- [ ] İzinler VERİLDİ (Yakındaki Cihazlar)
- [ ] GNSS alıcı AÇIK ve YAKIN (<1m)
- [ ] GNSS keşfedilebilir modda
- [ ] Başka Bluetooth cihaz bağlı değil

---

## 🆘 Hala Çalışmazsa

### Debug APK Oluştur ve Log Gönder
```cmd
# 1. Clean build
gradlew clean

# 2. Debug APK
gradlew assembleDebug --no-daemon --info > build_log.txt

# 3. APK yükle
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk

# 4. Logcat kaydet
adb logcat -c
adb logcat *:D > logcat.txt
# (Uygulamayı kullan, tarama/bağlantı dene)
# (Ctrl+C ile durdur)

# 5. Log dosyalarını gönder:
#    - build_log.txt
#    - logcat.txt
```

---

## 📞 İletişim

Sorun devam ederse lütfen şunları gönderin:
1. Telefon modeli ve Android versiyonu
2. GNSS alıcı markası ve modeli
3. Logcat çıktısı (yukarıdaki komutla)
4. Ekran görüntüleri (hata mesajları)

---

**Güncelleme:** 2025-11-03
**Sürüm:** TUGIS3 v1.0-debug
**Durum:** ✅ Build hataları düzeltildi, Bluetooth tarama ve bağlantı sistemi hazır

