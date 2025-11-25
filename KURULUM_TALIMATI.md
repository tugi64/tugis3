# KESİT PLAN APLİKASYONU - MANUEL KURULUM TALİMATI

## ⚠️ ÖNEMLİ: Dosya Değişimi Gerekiyor

Yeni Cross Section Measurement Activity hazır! Ancak manuel olarak aktive edilmesi gerekiyor.

## 📝 ADIM ADIM KURULUM

### Adım 1: Yedekleme (Opsiyonel)
```
1. Android Studio'da sol taraftan Files'ı açın
2. app/src/main/java/com/example/tugis3/ui/survey/ klasörüne gidin
3. CrossSectionMeasurementActivity.kt dosyasına sağ tıklayın
4. Copy → Paste → CrossSectionMeasurementActivity_OLD.kt olarak kaydedin
```

### Adım 2: Yeni Dosyayı Aktive Etme

**Seçenek A: IDE ile (Önerilen)**
```
1. CrossSectionMeasurementActivity_NEW.kt dosyasını açın
2. CTRL+A (Tümünü Seç)
3. CTRL+C (Kopyala)
4. CrossSectionMeasurementActivity.kt dosyasını açın
5. CTRL+A (Tümünü Seç)
6. CTRL+V (Yapıştır)
7. CTRL+S (Kaydet)
```

**Seçenek B: File Manager ile**
```
1. Windows Explorer'ı açın
2. Şu klasöre gidin:
   C:\Users\CASPER\AndroidStudioProjects\tugis3\app\src\main\java\com\example\tugis3\ui\survey\

3. CrossSectionMeasurementActivity.kt dosyasını silin (veya rename edin)
4. CrossSectionMeasurementActivity_NEW.kt dosyasını
   CrossSectionMeasurementActivity.kt olarak yeniden adlandırın
```

### Adım 3: Build
```
1. Android Studio'da Build → Clean Project
2. Build → Rebuild Project
3. Hata varsa düzeltin
```

### Adım 4: Test
```
1. Run → Run 'app'
2. Emulator veya cihazda açın
3. Ana Menü → Survey → Kesit Ölçümü
4. GPS izinlerini verin
5. Test edin!
```

## ✅ BAŞARILI OLDUĞUNU NASIL ANLARSINIZ?

Uygulama açıldığında şunları görmeli siniz:
- ✅ Top bar'da "Export" butonu (Save icon)
- ✅ GPS hassasiyet göstergesi
- ✅ Uydu sayısı göstergesi
- ✅ 2D Kesit grafiği
- ✅ "Nokta Ekle" ve "Oto Ölçüm" butonları

## 🐛 SORUN GİDERME

### Hata: "Duplicate class"
Çözüm: Eski yedek dosyaları silin (_OLD, _NEW)

### Hata: "Unresolved reference"
Çözüm: Build → Clean Project → Rebuild

### Hata: "GPS permission denied"
Çözüm: Settings → Apps → TUGIS3 → Permissions → Location → Allow

## 📞 YARDIM

Sorun yaşarsanız:
1. Hata mesajını kopyalayın
2. Dosya adını ve satır numarasını not edin
3. Geliştirici desteğine başvurun

---

**NOT:** Bu adımları tamamladıktan sonra Kesit Plan Aplikasyonu v2.0 tamamen çalışır durumda olacaktır!

