# 📱 UYGULAMA TEST REHBERİ

## ✅ APK BAŞARIYLA YÜKLENDİ!

Şimdi uygulamayı test ederken bu kontrol listesini kullanın.

---

## 🧪 TEST KONTROL LİSTESİ

### 1️⃣ İLK AÇILIŞ
- [ ] Uygulama açılıyor mu?
- [ ] Splash screen gösteriliyor mu?
- [ ] Ana ekran yükleniyor mu?
- [ ] Crash olmuyor mu?

### 2️⃣ İZİNLER (ÖNEMLİ!)
Uygulama bu izinleri isteyecek:
- [ ] Konum izni (GPS için)
- [ ] Bluetooth izni (RTK bağlantısı için)
- [ ] Depolama izni (dosya kaydetme için)
- [ ] Kamera izni (QR kod ve fotoğraf için)

**NOT:** Tüm izinleri "İZİN VER" seçin, yoksa bazı özellikler çalışmaz!

### 3️⃣ ANA MENÜ
- [ ] Proje menüsü açılıyor mu?
- [ ] Ölçü menüsü çalışıyor mu?
- [ ] Cihaz menüsü açılıyor mu?
- [ ] Araçlar menüsü görünüyor mu?

### 4️⃣ BLUETOOTH BAĞLANTISI
- [ ] Cihaz > Bluetooth Ayarları açılıyor mu?
- [ ] Bluetooth cihazları listeleniyor mu?
- [ ] RTK cihazına bağlanabiliyor musunuz?

### 5️⃣ PROJE YÖNETİMİ
- [ ] Yeni proje oluşturuluyor mu?
- [ ] Proje listesi görünüyor mu?
- [ ] Proje seçimi çalışıyor mu?

### 6️⃣ DOSYA YÖNETİMİ
- [ ] Dosya Yönet menüsü açılıyor mu?
- [ ] Nokta listesi içe aktarma çalışıyor mu?
- [ ] .ncn formatı destekleniyor mu?
- [ ] Dosyalar kaydediliyor mu?

### 7️⃣ NOKTA ÖLÇÜMÜ
- [ ] Nokta ölçüm ekranı açılıyor mu?
- [ ] GPS koordinatları alınıyor mu?
- [ ] Noktalar kaydediliyor mu?

### 8️⃣ CAD APLİKASYONU
- [ ] CAD ekranı açılıyor mu?
- [ ] Çizim araçları çalışıyor mu?
- [ ] Zoom in/out yapılabiliyor mu?
- [ ] Dosya açma/kaydetme çalışıyor mu?

---

## ⚠️ OLASI SORUNLAR VE ÇÖZÜMLER

### SORUN 1: Uygulama Crash Oluyor
**Belirti:** Uygulama açılır açılmaz kapanıyor
**Çözüm:**
```cmd
# Log almak için:
adb logcat | findstr tugis3 > crash_log.txt
```
Bu dosyayı inceleyin veya paylaşın.

### SORUN 2: GPS Çalışmıyor
**Belirti:** Koordinatlar 0.0 gösteriyor
**Çözüm:**
1. Konum izni verildi mi? ✅
2. GPS açık mı? (Ayarlar > Konum)
3. Açık havada test edin (bina içinde GPS zayıf)

### SORUN 3: Bluetooth Bağlanmıyor
**Belirti:** RTK cihazı görünmüyor
**Çözüm:**
1. Bluetooth izni verildi mi? ✅
2. Bluetooth açık mı?
3. RTK cihazı açık mı?
4. Daha önce eşleştirilmiş mi?

### SORUN 4: Dosya Kaydedilmiyor
**Belirti:** "Kaydetme başarısız" hatası
**Çözüm:**
1. Depolama izni verildi mi? ✅
2. Telefonda yeterli alan var mı?
3. Klasör oluşturulabiliyor mu?

### SORUN 5: Menüler Açılmıyor
**Belirti:** Menülere tıklayınca tepki yok
**Çözüm:**
1. Uygulamayı kapat-aç
2. Telefonu yeniden başlat
3. APK'yi tekrar yükle

---

## 🐛 HATA RAPORU ALMAK İÇİN:

### Yöntem 1: Logcat (Detaylı)
```cmd
adb logcat -c  # Önce temizle
adb logcat > app_log.txt  # Log kaydet
```
Sorunu tekrarla, sonra Ctrl+C ile dur.

### Yöntem 2: Crash Log (Sadece Crash)
```cmd
adb logcat *:E > error_log.txt
```

### Yöntem 3: Uygulama İçi Log
Bazı ekranlarda "Log" butonu olabilir, ona tıklayın.

---

## 📊 TEST SONUÇLARI ŞABLONU

Test ettikten sonra bu şablonu doldurun:

```
=== TEST RAPORU ===
Tarih: [...]
Telefon: [Model ve Android sürümü]
APK: tugis3-debug.apk

✅ ÇALIŞAN ÖZELLİKLER:
- [...]
- [...]

❌ ÇALIŞMAYAN ÖZELLİKLER:
- [...]
- [...]

⚠️ KISMÎ ÇALIŞAN:
- [...]

📝 NOTLAR:
- [...]
===================
```

---

## 🎯 ÖNCELİKLİ TEST EDİLMESİ GEREKENLER:

En önemli özellikler (sizin istediğiniz):

1. ✅ **Dosya Yönet Menüsü**
   - Ekran açılıyor mu?
   - İçe aktarma çalışıyor mu?
   - .ncn formatı destekleniyor mu?

2. ✅ **Nokta Listesi İçe Aktarma**
   - Tüm formatlar (.csv, .txt, .ncn) destekleniyor mu?
   - Noktalar doğru gösteriliyor mu?

3. ✅ **CAD Aplikasyonu**
   - Dosya açma çalışıyor mu?
   - Telefon hafızasına bağlanıyor mu?
   - Yandex Disk yerine local storage kullanılıyor mu?

4. ✅ **Nokta Aplikasyonu**
   - CAD benzeri özellikler var mı?
   - Dosya içe aktarma çalışıyor mu?

5. ✅ **Gezici/Rover Menüsü**
   - Cihaz menüsünde var mı?
   - Bluetooth bağlı değilse uyarı veriyor mu?
   - Marka/model seçimi çalışıyor mu?

---

## 💡 İYİLEŞTİRME ÖNERİLERİ

Test ederken bunları not edin:

1. **Hangi özellikler eksik?**
2. **Hangi butonlar çalışmıyor?**
3. **Hangi ekranlar yavaş?**
4. **Hangi mesajlar anlaşılmıyor?**
5. **UI/UX iyileştirmeleri?**

---

## 🚀 SONRAKI ADIMLAR

### Eğer Uygulama İyi Çalışıyorsa:
1. Release APK oluşturun (imzalı)
2. Play Store'a yüklemeye hazırlayın
3. Beta test grubu oluşturun

### Eğer Sorunlar Varsa:
1. Hataları kaydedin
2. Log dosyalarını toplayın
3. Sorunları bildirin
4. Düzeltmeler yapılsın

---

## 📞 DESTEK

Sorun olursa şunları paylaşın:
1. Telefon modeli ve Android sürümü
2. Hata mesajı (ekran görüntüsü)
3. Log dosyası (varsa)
4. Hangi özellikte hata oluştu

---

## ✅ BAŞARIYLA TEST EDİLDİ MI?

Testi tamamladıktan sonra:
- [ ] Test raporu dolduruldu
- [ ] Hatalar kaydedildi
- [ ] Log dosyaları alındı
- [ ] İyileştirme notları yazıldı

**Test sonuçlarını paylaşın!** 🎉

---

## 🎊 BAŞARILI TEST İÇİN KUTLAMALAR!

Eğer tüm özellikler çalışıyorsa:
```
🎉🎉🎉 TEBRİKLER! 🎉🎉🎉
Uygulama başarıyla çalışıyor!
```

Artık release APK oluşturabilir ve kullanıma sunabilirsiniz!

```cmd
gradlew assembleRelease --no-daemon
```

---

**İyi Testler!** 🚀

