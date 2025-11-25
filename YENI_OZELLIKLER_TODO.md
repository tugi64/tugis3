# YENİ ÖZELLİKLER TODO LİSTESİ

## 🎯 Öncelikli İstekler

### 1. 📋 DOSYA YÖNET Ekranı (Henüz Tamamlanmadı Mesajı)
**Durum:** ❌ Tamamlanacak
**İstenen:**
- Ana menüden "Dosya Yönet" açıldığında "Bu ekran henüz tamamlanmadı" mesajı gösteriyor
- Tam fonksiyonel Dosya Yönetimi ekranı yapılacak

**Özellikler:**
- [ ] Dosya listesi gösterimi
- [ ] Dosya arama
- [ ] Dosya silme
- [ ] Dosya paylaşma
- [ ] Dosya detayları
- [ ] Import/Export işlemleri

**Referans:**
- `kullanimklavuz.txt`, `kullanimklavuz2.txt`, `kullanimklavuz3.txt` dosyalarını oku

---

### 2. 📥 NOKTA İÇE AKTARMA - .NCN Format Desteği
**Durum:** ❌ Eklenecek
**İstenen:**
- Tüm nokta içe aktarma bölümlerinde .NCN format desteği olsun
- Kullanıcı her yerde .NCN dosyası aktarabilsin

**NCN Format Detayları:**
```
Uzantı: .ncn
Format: Boşluk ile ayrılmış (space delimited)
Sıralama: NoktaAdı Doğu Kuzey Yükseklik

Örnek:
m1 446055.966 4286420.511 1072.775
k1 446058.958 4286420.017 1073.039
k2 446058.659 4286412.615 1072.708
yol1 446059.797 4286401.519 1072.427
```

**Eklenecek Yerler:**
- [ ] Nokta Listesi > İçe Aktar
- [ ] Ölçüm > Nokta İçe Aktar
- [ ] Dosya Yönet > İçe Aktar
- [ ] Proje > Nokta İçe Aktar
- [ ] Tüm import dialogları

**Parser Fonksiyonu:**
```kotlin
fun parseNcnFile(content: String): List<Point> {
    return content.lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size >= 4) {
                Point(
                    name = parts[0],
                    easting = parts[1].toDoubleOrNull() ?: 0.0,
                    northing = parts[2].toDoubleOrNull() ?: 0.0,
                    elevation = parts[3].toDoubleOrNull() ?: 0.0
                )
            } else null
        }
}
```

---

### 3. 🛰️ CİHAZ > GEZİCİ Menüsü
**Durum:** ❌ Yeni eklenecek
**İstenen:**
- Cihaz menüsünde "Gezici" alt menüsü olsun
- Bluetooth bağlantısı yoksa "RTK bağlı değil" uyarısı
- Bluetooth bağlantısı varsa Gezici menüsü aktif

**Özellikler:**
- [ ] Bluetooth durumu kontrolü
- [ ] RTK bağlantı kontrolü
- [ ] Marka seçimi (Dropdown)
- [ ] Model seçimi (Marka bazlı)
- [ ] Gerçek cihaz bağlantısı

**Desteklenen Markalar ve Modeller:**

#### SOUTH
- Galaxy G1
- Galaxy G2 Plus
- Galaxy G3
- Inno7
- S82V
- S86T

#### TRIMBLE
- R2
- R10
- R12
- R12i
- SPS986

#### LEICA
- GS07
- GS14
- GS15
- GS16
- GS18

#### TOPCON
- HiPer HR
- HiPer VR
- HiPer SR
- Hiper V

#### CHC (CHCNAV)
- i70
- i80
- i90
- X20
- X900

#### HI-TARGET
- V90 Plus
- V100
- V200
- iRTK5

**UI Yapısı:**
```
Cihaz Menü
└── Gezici (Bluetooth bağlıysa aktif)
    ├── Marka Seçimi [Dropdown]
    ├── Model Seçimi [Dropdown - Markaya göre]
    ├── Bağlantı Ayarları
    │   ├── Port
    │   ├── Baud Rate
    │   └── Data Format
    ├── RTK Ayarları
    │   ├── Base/Rover Mode
    │   ├── NTRIP Settings
    │   └── Radio Settings
    └── [Bağlan] Button
```

---

### 4. 📱 CAD Aplikasyon - Dosya Açma Sorunu
**Durum:** ⚠️ Düzeltilecek
**Sorun:**
- CAD > Dosya Aç > İçe Aktar dendiğinde Yandex Disk açılıyor
- Telefonun kendi hafızası görünmüyor

**İstenen:**
- Telefon dahili hafızası (Internal Storage)
- SD Kart (External Storage)
- Downloads klasörü
- Documents klasörü

**Çözüm:**
```kotlin
// DocumentProvider ile dosya seçimi
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "*/*" // veya "application/dxf"
    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
        "application/dxf",
        "application/dwg",
        "text/plain"
    ))
}
startActivityForResult(intent, REQUEST_CODE_OPEN_FILE)
```

---

### 5. 📁 NOKTA Aplikasyon - CAD'e Benzer UI
**Durum:** ❌ Geliştirilecek
**İstenen:**
- Nokta aplikasyonunun ekranı CAD aplikasyonuna benzesin
- Aynı dosya açma/kaydetme mantığı
- Benzer araç çubuğu ve menüler

**Özellikler:**
- [ ] Nokta listesi grid görünümü
- [ ] Nokta filtreleme
- [ ] Nokta düzenleme
- [ ] Nokta ekleme/silme
- [ ] Export/Import (NCN dahil)
- [ ] Nokta arama
- [ ] Koordinat sistemleri arası dönüşüm

---

## 🐛 Mevcut Sorunlar

### 1. ⏰ Saat Gösterimi Donuyor
**Durum:** 🔍 Araştırılıyor
**Sorun:** Üst bardaki saat 20:20:12'de takılı kalıyor

**Geçici Çözümler:**
- Uygulamayı kapat-aç
- Telefonu yeniden başlat
- Clean build yap

**Olası Sebepler:**
- Compose recomposition sorunu
- State update sorunu
- Coroutine scope sorunu

---

### 2. 🔵 Bluetooth Tarama Bazen Çalışmıyor
**Durum:** ⚠️ İnceleniyor
**Sorun:** İletişim menüsünde "Tara" basınca tarama başlamıyor

**Kontrol Edilecek:**
- [ ] İzinler verilmiş mi?
- [ ] Konum servisi açık mı?
- [ ] Bluetooth açık mı?
- [ ] Başka uygulama Bluetooth kullanıyor mu?

---

### 3. 🔌 Bluetooth Eşleştirme Bildirimi Gelmiyor
**Durum:** ✅ DÜZELTİLDİ (Test edilecek)
**Yapılan:**
- ACTION_BOND_STATE_CHANGED receiver eklendi
- ACTION_ACL_CONNECTED receiver eklendi
- Toast bildirimleri eklendi

---

## 📝 Teknik Notlar

### NCN Parser Implementation
```kotlin
// app/src/main/java/com/example/tugis3/utils/NcnParser.kt
package com.example.tugis3.utils

import com.example.tugis3.data.db.entity.PointEntity
import java.io.File
import java.io.InputStream

object NcnParser {
    
    fun parseFromFile(file: File): Result<List<PointEntity>> {
        return runCatching {
            file.readLines().parseNcnLines()
        }
    }
    
    fun parseFromStream(stream: InputStream): Result<List<PointEntity>> {
        return runCatching {
            stream.bufferedReader().readLines().parseNcnLines()
        }
    }
    
    private fun List<String>.parseNcnLines(): List<PointEntity> {
        return this
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapIndexedNotNull { index, line ->
                try {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        PointEntity(
                            id = 0, // Will be auto-generated
                            name = parts[0],
                            easting = parts[1].toDouble(),
                            northing = parts[2].toDouble(),
                            elevation = parts[3].toDouble(),
                            description = if (parts.size > 4) parts.drop(4).joinToString(" ") else "",
                            code = "",
                            timestamp = System.currentTimeMillis()
                        )
                    } else null
                } catch (e: Exception) {
                    android.util.Log.e("NcnParser", "Error parsing line $index: $line", e)
                    null
                }
            }
    }
}
```

### Gezici (Rover) Model Structure
```kotlin
// app/src/main/java/com/example/tugis3/data/model/GnssManufacturer.kt
package com.example.tugis3.data.model

data class GnssManufacturer(
    val id: String,
    val name: String,
    val models: List<GnssModel>
)

data class GnssModel(
    val id: String,
    val name: String,
    val manufacturer: String,
    val connectionType: List<ConnectionType>,
    val defaultSettings: GnssSettings
)

enum class ConnectionType {
    BLUETOOTH, USB, SERIAL, NETWORK
}

data class GnssSettings(
    val baudRate: Int = 115200,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Parity = Parity.NONE,
    val nmeaVersion: String = "4.10",
    val updateRate: Int = 1 // Hz
)

enum class Parity {
    NONE, ODD, EVEN
}

// Data
object GnssManufacturers {
    val all = listOf(
        GnssManufacturer("south", "SOUTH", listOf(
            GnssModel("galaxy_g1", "Galaxy G1", "south", listOf(ConnectionType.BLUETOOTH), GnssSettings()),
            GnssModel("galaxy_g2", "Galaxy G2 Plus", "south", listOf(ConnectionType.BLUETOOTH), GnssSettings()),
            // ...
        )),
        GnssManufacturer("trimble", "Trimble", listOf(
            GnssModel("r2", "R2", "trimble", listOf(ConnectionType.BLUETOOTH), GnssSettings()),
            // ...
        )),
        // ...
    )
}
```

---

## 🚀 Uygulama Hedefleri

### Versiyon 1.1 (Öncelikli)
- [x] Bluetooth eşleştirme düzeltmesi
- [ ] NCN format desteği
- [ ] Dosya Yönet ekranı
- [ ] Gezici menüsü

### Versiyon 1.2 (Sonra)
- [ ] CAD dosya açma düzeltmesi
- [ ] Nokta aplikasyon geliştirme
- [ ] Saat donması çözümü
- [ ] Bluetooth güvenilirlik iyileştirmeleri

### Versiyon 1.3 (Gelecek)
- [ ] Auto-reconnect
- [ ] Background GNSS logging
- [ ] Cloud sync
- [ ] Multi-user support

---

## 📞 İletişim & Destek

**Build Komutları:**
```cmd
# Hızlı build
HIZLI_BUILD.bat

# Temiz build
gradlew clean assembleDebug

# Release build
gradlew assembleRelease

# Nuclear clean
nuclear_clean.bat
```

**Log Komutları:**
```cmd
# Bluetooth logları
adb logcat | findstr "Bluetooth"

# Uygulama logları
adb logcat | findstr "Tugis3"

# Hata logları
adb logcat *:E
```

---

**Son Güncelleme:** 2025-01-02
**Durum:** Aktif Geliştirme 🚧

