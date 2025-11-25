# Hat Aplikasyonu Geliştirmeleri

## Yapılan İyileştirmeler ve Özellikler

### 1. Çizgi Parametreleri Sekmesi (Yeni)
Kullanım kılavuzuna uygun olarak hat tanımlama ekranı iki sekmeye ayrıldı:

#### Özellikler:
- **Hat İsmi (İsim)**: Hat'a özel bir isim verilebilir veya otomatik oluşturulur
- **Kilometre Gir**: Eğer bir başlangıç km'si var ise tanımlanabilir
- **Giriş Tipi**: Başlangıç ve Bitiş Noktası Ayarla seçenekleri
  - Konum oku veya Nokta Listesi seçenekleriyle nokta seçimi
  - Seçilen noktaların X, Y koordinatları gösterilir
- **Nokta Adı Gösterimi**: Başlangıç (Y) ve Bitiş (X) nokta isimleri görüntülenir

### 2. Aplikasyon Ayarları Sekmesi (Yeni)
İkinci sekme aplikasyon özel ayarlarını içerir:

#### Özellikler:
- **Noktaları veya Çizgileri Kullan**: Switch ile aktif/pasif edilir
  - Hatta özel aralıkta tanımlama veya belirli metrajda bölme için kullanılır

- **Hesaplama Modu Seçenekleri**:
  1. **Tam Sayı Modu** (EQUAL_DIVISION)
     - Hattı seçilen sayı kadar eşit parçaya böler
  2. **Tam Sayı Mesafe Modu** (FIXED_DISTANCE)
     - Hattı seçilen mesafeye göre böler (örn: her 5 metrede)
  3. **Özel Kilometre Ofseti** (CUSTOM_KM)
     - Sadece başlangıç ve bitiş noktalarını kullanır

- **Kilometre (Böl)**: 
  - Bölme değeri (metre cinsinden)
  - Kesitli eğim açıklaması eklendi

- **Toplam Uzunluk Gösterimi**: 
  - Hat toplam uzunluğu metre cinsinden
  - Başlangıç - Bitiş km aralığı gösterilir
  - Örnek: `523.456 m (0.000 - 0.523)`

- **Kesitli Eğim (%)**: 
  - Başlangıç noktasından bitiş noktasına belirli eğimle gitmek için

- **Otomatik En Yakın Nokta Aplikasyonu**: 
  - Switch ile aktif/pasif edilir
  - Aktif olduğunda seçilen aralıkta oluşturulan ara noktalara göre en yakındaki nokta otomatik seçilir

### 3. Otomatik En Yakın Nokta Özelliği
ViewModel'de `updateDistancesAndBearings()` fonksiyonu güncellendi:
```kotlin
// Otomatik en yakın nokta seçimi
val selectedLine = _selectedLine.value
if (selectedLine?.autoNearestPoint == true && _stakeoutPoints.value.isNotEmpty()) {
    val nearestIndex = _stakeoutPoints.value.withIndex().minByOrNull { it.value.distance }?.index
    if (nearestIndex != null && nearestIndex != _selectedPointIndex.value) {
        _selectedPointIndex.value = nearestIndex
    }
}
```

### 4. Aplikasyon Ekranı İyileştirmeleri

#### Otomatik En Yakın Nokta Göstergesi:
- Özellik aktifse ekran üstünde bilgilendirme banner'ı gösterilir
- Yeşil-turuncu renkli görsel gösterge
- "Otomatik En Yakın Nokta Aplikasyonu Aktif" yazısı

#### Anlık Hat Durumu Gösterimi (Büyük Gösterim):
- **Hat KM**: Güncel konumun hat üzerindeki km değeri
- **OFSET**: Hat'a olan uzaklık (Sol/Sağ işaretli)
  - Yeşil: Hatta (< 5 cm)
  - Sarı: Hata yakın (5-20 cm)
  - Kırmızı: Hattan uzak (> 20 cm)
- İki sütunlu, büyük fontlu gösterim
- Ofset yönü: SAĞ (+) / SOL (-) işareti ile

#### Navigasyon Butonları:
- **Nokta Sayacı**: "Nokta X / Y" göstergesi eklendi
- **İleri/Geri Butonları**: 
  - Daha büyük ikonlar (24.dp)
  - Renkli arka plan (secondary container ve primary)
  - Bold font
  - İkon + Metin kombinasyonu
- **Kapat Butonu**: Outlined stil ile

### 5. Hat Seçim Listesi Ekranı

#### Yeni Başlık Alanı:
- "Hat Seçim Listesi" başlığı
- Sağ üstte "Hat Seçim Listesi" göstergesi (badge)
- Liste ikonu ile görsel zenginleştirme
- Hat ismi büyük ve belirgin
- Seçili nokta bilgileri: Nokta, Km, Z, Mesafe, Azimut

### 6. Görsel İyileştirmeler
- Sekmeli (Tab) dialog yapısı
- Renkli Surface bileşenleri ile kategorize edilmiş alanlar
- Daha detaylı açıklama metinleri
- Radio button'lar için arkaplan rengi
- Daha iyi hiyerarşi ve görsel ayrım

## Kullanım Senaryoları

### Senaryo 1: Sabit Mesafe İle Hat Aplikasyonu
1. Hat tanımla (Başlangıç ve Bitiş noktası seç)
2. "Aplikasyon Ayarları" sekmesine geç
3. "Noktaları veya Çizgileri Kullan" aktif et
4. "Tam Sayı Mesafe Modu" seç
5. Kilometre alanına "5" gir (her 5 metrede bir nokta)
6. Tamam'a tıkla
7. Hat listesinden seçip "Aplikasyon"a başla

### Senaryo 2: Otomatik En Yakın Nokta İle Çalışma
1. Hat tanımlarken "Aplikasyon Ayarları" sekmesinde
2. "Otomatik En Yakın Nokta Aplikasyonu" switch'ini aktif et
3. Aplikasyon başlatıldığında sistem otomatik olarak en yakın noktayı seçer
4. Kullanıcı hareket ettikçe otomatik nokta değişir

### Senaryo 3: Kesitli Eğim İle Hat
1. Hat tanımla
2. "Kesitli Eğim (%)" alanına eğim değeri gir (örn: 2.5)
3. Sistem Z koordinatlarını eğime göre hesaplar

## Teknik Detaylar

### Değişen Dosyalar:
- `LineStakeoutActivity.kt`: Tüm UI ve ViewModel değişiklikleri

### Yeni State'ler:
- `currentTab`: Dialog sekme seçimi (0: Çizgi Parametreleri, 1: Aplikasyon Ayarları)

### Güncellenen Fonksiyonlar:
- `LineEditDialog`: Sekmeli yapıya dönüştürüldü
- `updateDistancesAndBearings()`: Otomatik en yakın nokta seçimi eklendi
- `ApplicationScreen`: Görsel iyileştirmeler ve yeni göstergeler
- `StakeoutView`: Başlık alanı zenginleştirildi

### Mevcut Özellikler (Korundu):
- Hat km ve ofset hesaplama
- GNSS konum takibi
- Harita gösterimi
- Fix type göstergesi
- Nokta listesi ve seçimi
- İleri/Geri navigasyon

## Test Önerileri

1. **Tab Geçişi**: Dialog'da iki sekme arasında geçiş yapın
2. **Nokta Seçimi**: Başlangıç ve bitiş noktası seçim ekranlarını test edin
3. **Hesaplama Modları**: Üç farklı hesaplama modunu test edin
4. **Otomatik Seçim**: Otomatik en yakın nokta özelliğini aktif edip hareket edin
5. **Görsel Kontrol**: Ofset göstergesinin renk değişimini test edin
6. **Navigasyon**: İleri/Geri butonlarının çalışmasını kontrol edin

## Görüntülerden Alınan Özellikler

✅ Çizgi Parametreleri ekranı
✅ Aplikasyon Ayarları ekranı
✅ Noktaları ve Çizgileri Kullan switch'i
✅ Hesaplama modu seçenekleri (3 mod)
✅ Kilometre girişi
✅ Toplam uzunluk gösterimi
✅ Kesitli Eğim (%)
✅ Otomatik en yakın nokta aplikasyonu
✅ Hat km ve ofset gösterimi
✅ İleri/Geri navigasyon butonları
✅ Aplikasyon aralığı seçme menüsü
✅ Hat Seçim listesi başlığı

## Sonuç
Kullanım kılavuzundaki tüm özellikler uygulamaya başarıyla entegre edildi. Uygulama artık profesyonel arazi ölçüm yazılımlarına yakın bir kullanıcı deneyimi sunuyor.

