# TUGIS3 Ürün Backlog

Kaynaklar:
- `kullanimklavuz.txt` (TR — SurPad 4.0 odaklı)
- `kullanimklavuzu2.txt` (EN — SurPad kapsamlı içerik)

Aşağıdaki maddeler; MVP1 (çalışır temel akış), MVP2 (saha kullanımına uygun genişletmeler) ve + (ileri seviye) olarak etiketlenmiştir. P0/1/2 önceliklendirmesi eklendi.

---
## EPIC A — Proje ve Veri Yönetimi
- [P0][MVP1] Proje oluşturma/açma/seçme (Project Manager)
- [P0][MVP1] Aktif proje değiştir (GNSS ekranı alt sayfası)
- [P1][MVP1] Proje dışa/ice aktar (zip klasörü)
- [P1][MVP1] Proje verisi dosya yöneticisi (PD dosyası + birden fazla data file switch)
- [P0][MVP1] Koordinat Sistemi Parametreleri
  - Öntanımlı projeksiyon (UTM, Gauss Krüger, TM)
  - Elipsoid seçimi (WGS84, GRS80, ED50 vs.)
  - 7-par, 4-par; yükseklik dönüşümü parametreleri
  - Yerel ofsetler, grid ve geoid dosyası desteği (dosya seçici)
- [P2][MVP2] Kordinat param dışa/ice aktar (lokal/QR/Cloud)
- [P1][MVP2] Nokta veri tabanı (listele/ara/ekle/düzenle/sil/geri al/filtrele/paylaş)
- [P1][MVP2] Çıktı al (csv, txt, dxf, kml/kmz, gpx, ncn, crd, html, excel, kullanıcı formatı)

## EPIC B — Cihaz & Haberleşme
- [P0][MVP1] Haberleşme Modları: Bluetooth, Wi‑Fi (WLAN), Seri, Demo
- [P0][MVP1] Üretici/Model seçim listesi (South, Trimble, Leica, Topcon, South ALPS2, South S82 vb.)
- [P0][MVP1] Rover/Base/Static mod ekranları (çekirdek alanlar + Apply)
- [P0][MVP1] Data Link seçenekleri (NTRIP/Device Internet, Phone Internet, External Radio, Internal Radio)
- [P0][MVP1] NTRIP profil yöneticisi (ekle/düzenle/sil/auto-connect/bağlan‑dur)
- [P1][MVP2] Gelişmiş ayarlar (cut‑off angle, PDOP limit, aRTK, sistem seçimi)
- [P1][MVP2] Çalışma modu durumu (özet panel)
- [P2][+ ] Konfigürasyon setleri (kaydet/uygula/QR paylaş)
- [P2][+ ] Dahili/harici radyo protokolleri ve frekans kanalları (bilgi/temel ayar)

## EPIC C — GNSS İzleme & Kayıt
- [P0][MVP1] GNSS izleme kartı (FixType, lat/lon/h, kullanılan/görülen uydu, HRMS/VRMS, yaş)
- [P0][MVP1] NMEA log aç/kapa, dosyaya yazım
- [P0][MVP1] “Noktayı Kaydet” (aktif projeye kayıt) – isim üretimi, kod alanı opsiyonel
- [P1][MVP2] FixType istatistikleri (sayaç/sıfırla)
- [P1][MVP2] Otomatik kayıt koşulları (HRMS/VRMS/PDOP eşikleri, epok süresi)
- [P2][+ ] IMU tilt ölçüm entegrasyonu (temel kalibrasyon akışı)

## EPIC D — Ölçüm & Aplikasyon
- [P1][MVP2] Hızlı Alım (sade arayüz – hassasiyet uyarıları)
- [P1][MVP2] Grafik Alım (harita + CAD arkaplan; katman yönetimi)
- [P1][MVP2] Nokta Aplikasyon
- [P2][+ ] Hat/Çizgi Aplikasyon
- [P2][+ ] Yol Stake‑out, kesit ölçümü
- [P2][+ ] GIS veri toplama (öznitelik formları)

## EPIC E — Araçlar
- [P1][MVP2] Lokalizasyon (baz/marker point calibration + rapor)
- [P1][MVP2] Koordinat Dönüştürücü (tek nokta/dosya)
- [P2][+ ] Açı dönüştürücü, çevre/alan, COGO, hacim hesabı, grid‑to‑ground, offset üretimi
- [P2][+ ] FTP paylaşım, QR ile param aktarım

## EPIC F — Altyapı / Teknik Borç
- [P0][MVP1] Hilt modülleri (tekil DAO/Repo binding — duplicate fix)
- [P0][MVP1] Room schema export (KSP arg ile) – `app/schemas/`
- [P0][MVP1] NTRIP istemcisi (socket + simülasyon) ve GnssEngine’e NMEA besleme
- [P1][MVP2] Global crash handler + dosya log görüntüleyici
- [P1][MVP2] Compose M3 ile stabil API kullanımı, deneyselleri opt‑in
- [P1][MVP2] Export/Import dosya izinleri ve SAF entegrasyonu

---
## Milestone Planı

### MVP1 (Çalışır Temel Akış)
1. Proje oluştur/aç + Aktif Proje seçimi (GNSS ekran bottom sheet)
2. GNSS İzleme kartı + Nokta kaydetme
3. NTRIP Profilleri (liste, auto‑connect, bağlan/dur) – simülasyon ve gerçek
4. Cihaz haberleşme ekranı (Bluetooth/WLAN/Demo)+ basit tarama simülasyonu
5. Hilt/Room/KSP konfigürasyonları, Crashlog temel ekranı

### MVP2 (Saha Kullanımı)
1. Nokta DB (liste/ekle/düzenle/sil/filtre/geri al/paylaş)
2. Hızlı Alım, Grafik Alım (katman, CAD import), Nokta/Çizgi aplikasyon
3. Lokalizasyon ve Koordinat Dönüştürücü ekranları
4. Export formatları (CSV/TXT/DXF/KML/KMZ/GPX/NCN/CRD/HTML/XLS ve kullanıcı tanımlı)
5. Gelişmiş cihaz ayarları (cutoff/pdop/aRTK/gövde sistemleri)

---
## Notlar
- South ALPS2 ve South S82 cihaz isimleri üretici/model listesine eklenecek.
- NTRIP gerçek bağlantıda basit GET yetkilendirme ve RTCM/NMEA akışı; ileride robust hale getirilecek.
- Geoid/Grid dosyaları için dosya seçici ve önbellek klasörü planlanacak.
- Tüm büyük ekranlar Manifest’e eklenmiş olmalı (NtripProfilesActivity eklendi).

