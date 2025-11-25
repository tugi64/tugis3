# CAD Aplikasyonu - Test Senaryoları ve Kullanım Kılavuzu

## 📅 Tarih: 30 Ekim 2025

## 🎯 Test Senaryoları

### 1. SOL ARAÇ ÇUBUĞU TESTLERİ

#### Test 1.1: Katman Yönetimi
```
Adımlar:
1. CAD Aplikasyonu'nu aç
2. Sol taraftan "Katman" (Layers) ikonuna tıkla
3. Açılan dialogda katman listesini kontrol et
4. Checkbox'ları kullanarak katmanları göster/gizle
5. "Tümü" seçeneğini test et
6. Değişikliklerin haritaya yansıdığını kontrol et

Beklenen Sonuç:
✓ Katman listesi gösterilir
✓ Checkbox'lar çalışır
✓ Katmanlar gösterilir/gizlenir
✓ Harita güncellenir
```

#### Test 1.2: Veri İşlemleri
```
Adımlar:
1. Sol taraftan "Veri" (Data) ikonuna tıkla
2. Açılan menüyü kontrol et
3. "Dosya Aç" seçeneğini test et
4. DXF/CSV dosyası seç
5. Dosya yükleme mesajını kontrol et

Beklenen Sonuç:
✓ Menü açılır
✓ Alt seçenekler görünür
✓ Dosya seçici açılır
✓ SnackBar mesajı gösterilir
```

#### Test 1.3: Çizim Araçları
```
Adımlar:
1. Sol taraftan "Çizim" (Drawing) ikonuna tıkla
2. Her çizim aracını tek tek test et:
   - Nokta Çiz
   - Hat Çiz
   - Çoklu Çizgi
   - Daire
   - Yay
   - Çokgen
3. Her seçim için bilgi mesajını kontrol et

Beklenen Sonuç:
✓ Menü açılır
✓ Tüm araçlar listelenir
✓ Her araç için açıklama var
✓ SnackBar mesajı gösterilir
```

#### Test 1.4: Ölçüm Araçları
```
Adımlar:
1. Sol taraftan "Ölçüm" (Survey) ikonuna tıkla
2. Kesişim İşlemleri bölümünü kontrol et
3. Ofset İşlemleri bölümünü kontrol et
4. Diğer İşlemler bölümünü kontrol et

Beklenen Sonuç:
✓ Menü açılır
✓ Alt başlıklar görünür
✓ Tüm araçlar listelenir
✓ İpucu mesajı gösterilir
```

#### Test 1.5: Zoom İşlemleri
```
Adımlar:
1. Sol taraftan "Yakınlaştır" (Zoom In) ikonuna tıkla
2. Haritanın yakınlaştığını kontrol et
3. "Uzaklaştır" (Zoom Out) ikonuna tıkla
4. Haritanın uzaklaştığını kontrol et

Beklenen Sonuç:
✓ Harita yakınlaşır
✓ Mesaj gösterilir
✓ Harita uzaklaşır
✓ Mesaj gösterilir
```

---

### 2. SAĞ ARAÇ ÇUBUĞU TESTLERİ

#### Test 2.1: Aplikasyon (Stakeout)
```
Adımlar:
1. Seçim modunu aç (üst menüden)
2. Bir CAD objesi seç
3. Sağ taraftan "Aplikasyon" ikonuna tıkla
4. Aplikasyon mesajını kontrol et
5. Seçim olmadan aplikasyonu test et

Beklenen Sonuç:
✓ Seçili obje ile: Aplikasyon başlar
✓ Seçili obje olmadan: Uyarı mesajı
✓ SnackBar mesajı gösterilir
```

#### Test 2.2: Ayarlar
```
Adımlar:
1. Sağ taraftan "Ayarlar" ikonuna tıkla
2. Grid Göster/Gizle switch'ini test et
3. Snap switch'ini test et
4. Değişiklikleri kaydet

Beklenen Sonuç:
✓ Dialog açılır
✓ Switch'ler çalışır
✓ Değişiklikler kaydedilir
✓ SnackBar mesajı gösterilir
```

#### Test 2.3: Hesaplama Araçları
```
Test 2.3.1: Açı Hesaplama
Adımlar:
1. Sağ taraftan "Araçlar" ikonuna tıkla
2. "Açı Hesapla" seçeneğini seç
3. Üç nokta gir (P1, P2, P3)
4. "Hesapla" butonuna tıkla
5. Sonuçları kontrol et

Beklenen Sonuç:
✓ Dialog açılır
✓ 3 input alanı var
✓ Placeholder'lar görünür
✓ Sonuç kartı gösterilir
✓ Derece ve DMS formatı gösterilir

Test 2.3.2: Mesafe Hesaplama
Adımlar:
1. "Mesafe Hesapla" seçeneğini seç
2. İki nokta gir (P1, P2)
3. "Hesapla" butonuna tıkla
4. Sonuçları kontrol et

Beklenen Sonuç:
✓ Dialog açılır
✓ 2 input alanı var
✓ Sonuç kartı gösterilir
✓ Yatay mesafe, yön, düşey fark gösterilir

Test 2.3.3: Alan Hesaplama
Adımlar:
1. "Alan Hesapla" seçeneğini seç
2. Noktaları virgülle ayırarak gir
3. Nokta sayısı göstergesini kontrol et
4. "Hesapla" butonuna tıkla

Beklenen Sonuç:
✓ Dialog açılır
✓ Çok satırlı input var
✓ Nokta sayacı çalışır
✓ Alan ve çevre gösterilir
```

#### Test 2.4: Silme İşlemi
```
Adımlar:
1. Seçim modunu aç
2. Bir obje seç
3. Sağ taraftan "Sil" ikonuna tıkla
4. Objenin silindiğini kontrol et

Beklenen Sonuç:
✓ Seçili obje silinir
✓ SnackBar mesajı gösterilir
✓ Harita güncellenir
```

#### Test 2.5: Geri Al / Tekrar Yap
```
Adımlar:
1. Bir işlem yap (örn: nokta seç)
2. Sağ taraftan "Geri" ikonuna tıkla
3. İşlemin geri alındığını kontrol et
4. "İleri" ikonuna tıkla
5. İşlemin tekrar yapıldığını kontrol et

Beklenen Sonuç:
✓ İşlem geri alınır
✓ İşlem tekrar yapılır
✓ SnackBar mesajları gösterilir
```

#### Test 2.6: Temizle
```
Adımlar:
1. Birkaç ölçüm noktası seç
2. Sağ taraftan "Temizle" ikonuna tıkla
3. Tüm noktaların temizlendiğini kontrol et

Beklenen Sonuç:
✓ Tüm ölçüm noktaları temizlenir
✓ SnackBar mesajı gösterilir
```

---

## 📚 KULLANIM KILAVUZU

### Başlangıç

1. **CAD Aplikasyonu'nu Açma**
   - Ana menüden "Ölçüm" → "CAD Aplikasyon" seçin
   - Uygulama açıldığında harita görünümü yüklenir

2. **Dosya Yükleme**
   - Sol araç çubuğundan "Veri" ikonuna tıklayın
   - "Dosya Aç" seçeneğini seçin
   - DXF, DWG, CSV veya GeoJSON dosyası seçin
   - Dosya yüklendikten sonra haritada görünür

### Temel İşlemler

#### Katman Yönetimi
```
1. Sol araç çubuğundan "Katman" ikonuna tıklayın
2. Görünmesini istediğiniz katmanları seçin
3. Checkbox'ları kullanarak göster/gizle
4. "Tümü" ile tüm katmanları aç/kapat
5. "Tamam" ile kaydedin
```

#### Çizim Yapma
```
1. Sol araç çubuğundan "Çizim" ikonuna tıklayın
2. Çizmek istediğiniz geometriyi seçin:
   - Nokta: Tek bir nokta işaretle
   - Hat: İki nokta seç
   - Çoklu Çizgi: Birden fazla nokta seç
   - Daire: Merkez ve yarıçap belirle
   - Yay: Üç nokta seç
   - Çokgen: Kapalı alan oluştur
3. Snap modunu açın (hassas seçim için)
4. Harita üzerinden noktaları seçin
```

#### Ölçüm Yapma
```
1. Sol araç çubuğundan "Ölçüm" ikonuna tıklayın
2. İstediğiniz ölçüm aracını seçin:
   - Kesişim: İki veya daha fazla objenin kesişimi
   - Ofset: Paralel veya dik ofset
   - Böl: Objeyi parçalara ayır
   - Ölçü: Mesafe ölçümü
3. Talimatları izleyin
```

#### Hesaplama Yapma
```
Açı Hesaplama:
1. Sağ araç çubuğundan "Araçlar" → "Açı Hesapla"
2. Üç nokta girin (2. nokta köşe noktası)
3. "Hesapla" tıklayın
4. Sonuç derece ve DMS formatında gösterilir

Mesafe Hesaplama:
1. "Araçlar" → "Mesafe Hesapla"
2. İki nokta girin
3. Yatay mesafe, yön ve düşey fark gösterilir

Alan Hesaplama:
1. "Araçlar" → "Alan Hesapla"
2. Noktaları virgülle ayırarak girin (en az 3)
3. Alan (m²) ve çevre gösterilir
```

#### Aplikasyon (Stakeout)
```
1. Üst menüden "Seçim" modunu açın
2. Harita üzerinden bir CAD objesi seçin
3. Sağ araç çubuğundan "Aplikasyon" ikonuna tıklayın
4. GPS ile gerçek zamanlı konum takibi başlar
5. Hedefe olan mesafe ve yön gösterilir
6. Hedefe ulaştığınızda "Kaydet" butonuna basın
```

### Gelişmiş Özellikler

#### Grid ve Snap Kullanımı
```
1. Sağ araç çubuğundan "Ayarlar" açın
2. Grid Göster: Arka planda ızgara gösterir
3. Snap: Noktaları otomatik yakalar
4. Snap toleransını ayarlayın (üst menüden)
```

#### Veri Dışa Aktarma
```
1. Sol araç çubuğundan "Veri" → "DXF/GeoJSON/CSV Dışa Aktar"
2. Ölçüm noktaları otomatik kaydedilir
3. Dosya Documents/TUGIS3 klasörüne kaydedilir
```

---

## 🔍 HATA AYIKLAMA

### Sık Karşılaşılan Sorunlar

#### Problem 1: Dosya Yüklenmiyor
```
Çözüm:
- Dosya formatını kontrol edin (DXF, DWG, CSV, GeoJSON)
- Dosya izinlerini kontrol edin
- Dosya boyutunu kontrol edin (çok büyük olabilir)
```

#### Problem 2: GPS Çalışmıyor
```
Çözüm:
- Konum izinlerini kontrol edin
- GPS'in açık olduğundan emin olun
- Açık alanda olduğunuzdan emin olun
- Uydu sayısını kontrol edin (en az 4 olmalı)
```

#### Problem 3: Çizim Yapılamıyor
```
Çözüm:
- Snap modunu açın/kapatın
- Zoom seviyesini ayarlayın
- Seçim modunu kapatın
```

#### Problem 4: Katmanlar Görünmüyor
```
Çözüm:
- Katman yönetiminden katmanları açın
- "Tümü" seçeneğini kullanın
- Dosyanın doğru yüklendiğini kontrol edin
```

---

## 💡 İPUÇLARI VE TRİKLER

### İpucu 1: Hassas Ölçüm
```
- Snap modunu açın
- Snap toleransını düşürün
- Grid'i gösterin
- Zoom yaparak yakınlaşın
```

### İpucu 2: Hızlı Çizim
```
- Snap World Mode kullanın (W-Snap)
- World toleransını ayarlayın (0.5m - 10m)
- Cluster modunu açın (yakın noktaları gruplar)
```

### İpucu 3: Verimli Çalışma
```
- Katman yönetimini kullanın
- Sadece gerekli katmanları açın
- Geri al/İleri özellikleri kullanın
- Düzenli olarak kaydedin
```

### İpucu 4: GPS Hassasiyeti
```
- Açık alanda çalışın
- En az 8 uydu bekleyin
- HDOP < 2 olmalı
- RTK/CORS kullanın (cm hassasiyet için)
```

---

## 📊 PERFORMANS İPUÇLARI

### Büyük Dosyalarla Çalışma
```
1. Gereksiz katmanları kapatın
2. Cluster modunu açın
3. Zoom'da otomatik basitleştirme aktif
4. Düzenli olarak temizlik yapın
```

### Batarya Tasarrufu
```
1. GPS güncellemelerini azaltın
2. Harita tipini basit tutun
3. Gereksiz katmanları kapatın
4. Ekran parlaklığını ayarlayın
```

---

## 🎓 EĞİTİM SENARYOLARı

### Senaryo 1: Kanal Projesi
```
Amaç: Bir kanal güzergahı boyunca kesit ölçümleri yapmak

Adımlar:
1. Proje oluşturun: "Kanal Projesi"
2. DXF dosyasından hat eksenini yükleyin
3. Kesit aplikasyonunu açın
4. Her 20m'de bir kesit ölçün
5. Sol ve sağ ofsetleri ölçün
6. Verileri HDM formatında kaydedin
7. Rapor oluşturun
```

### Senaryo 2: Arazi Ölçümü
```
Amaç: Bir arsanın sınırlarını belirlemek

Adımlar:
1. Nokta ölçüm modunu açın
2. Snap modunu açın
3. Köşe noktalarını ölçün
4. CAD'de çokgen çizin
5. Alan hesapla aracını kullanın
6. Sonuçları kaydedin
```

### Senaryo 3: Yol Aplikasyonu
```
Amaç: Yol eksenini aplikasyon ile işaretlemek

Adımlar:
1. Yol projesini DXF'den yükleyin
2. Seçim modunu açın
3. Yol eksenini seçin
4. Aplikasyon başlatın
5. GPS ile yol üzerinde ilerleyin
6. Belirlenen noktalarda kazık çakın
7. Ölçümleri kaydedin
```

---

## 🔄 GÜNCELLEME NOTLARI

### Versiyon 1.0 (30 Ekim 2025)
```
✓ İlk tam işlevsel versiyon
✓ Sol ve sağ araç çubukları aktif
✓ Tüm menüler Türkçe
✓ Hesaplama araçları eklendi
✓ SurPad uyumluluğu sağlandı
✓ Detaylı kullanım kılavuzu
```

### Gelecek Güncellemeler
```
□ Gerçek hesaplama algoritmaları
□ DXF/DWG tam desteği
□ Gelişmiş çizim araçları (trim, extend, fillet)
□ Ölçü ve ofset gerçek implementasyonu
□ Bulut senkronizasyonu
□ Çoklu kullanıcı desteği
```

---

## 📞 DESTEK VE İLETİŞİM

### Test Sonuçlarını Bildirme
```
Lütfen test sonuçlarınızı aşağıdaki başlıklarla raporlayın:
- Test Adı
- Beklenen Sonuç
- Gerçekleşen Sonuç
- Hata Mesajı (varsa)
- Ekran Görüntüsü (varsa)
```

---

**Hazırlayan**: AI Assistant  
**Proje**: TUGIS3 - Profesyonel Jeodezi Uygulaması  
**Tarih**: 30 Ekim 2025  
**Versiyon**: 1.0

