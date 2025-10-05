package com.example.tugis3.ui.common

object HelpContentProvider {
    data class HelpContent(
        val title: String,
        val intro: String,
        val steps: List<String> = emptyList(),
        val tips: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val devNote: String? = null
    )

    private val map: Map<String, HelpContent> = HashMap<String, HelpContent>().also { m ->
        fun add(key: String, intro: String, steps: List<String> = emptyList(), tips: List<String> = emptyList(), warnings: List<String> = emptyList(), devNote: String? = null) {
            m[key] = HelpContent(key, intro, steps, tips, warnings, devNote)
        }
        add(
            key = "Proje Bilgisi",
            intro = "Projeyi oluşturma/seçme, meta ve koordinat sistemi yönetimi.",
            steps = listOf("Yeni proje oluştur", "Koordinat sistemini seç veya içe al", "Gerekliyse elipsoid/projeksiyon özelleştir", "Yedek için düzenli dışa aktar"),
            tips = listOf("Depolama alanını kontrol et", "Tekrar isim çakışmasını önle"))
        add(
            key = "Dosya Yönet",
            intro = "Veri dosyası oluşturma, içe/dışa aktarma ve format seçimi.",
            steps = listOf("Yeni veri dosyası yarat", "Harici dosyayı klasöre kopyalayıp içe aktar", "Format & açı birimi seçerek dışa aktar"),
            tips = listOf("İsimde tarih kullan"))
        add(
            key = "Koordinat Sistemi",
            intro = "Elipsoid, projeksiyon, dönüşüm ve jeoit/grid tanımı.",
            steps = listOf("Hazır sistem seç veya yerel parametre gir", "Gerekirse jeoit ve grid dosyası ekle", "Ofset / dönüşüm parametrelerini doğrula", "Parametreleri dışa aktar"),
            warnings = listOf("Yanlış datum küresel kaydırma yapar"))
        add(
            key = "Nokta Kalibre",
            intro = "Yerelleştirme / kalibrasyon noktalarından dönüşüm üretimi.",
            steps = listOf("≥4 nokta ölç", "Hatalı sapmalı noktaları ele", "Çözümü uygula ve kilitle"),
            tips = listOf("Noktaları alana dengeli dağıt"))
        add(
            key = "Nokta Listesi",
            intro = "Noktaları ara, düzenle, filtrele ve paylaş.",
            steps = listOf("Ada/koda göre filtrele", "Düzenle veya sil", "Toplu dışa aktar / QR paylaş"))
        add(
            key = "Veri Gönder",
            intro = "Nokta ve çizim verilerini çeşitli formatlarda dışa aktar.",
            steps = listOf("Kaynak dosyayı seç", "Format & açı birimi seç", "Hedef yolu onayla"))
        add(
            key = "Barkod",
            intro = "QR ile parametre / profil alma.",
            steps = listOf("Kameraya izin ver", "Kodu tara ve uygula"))
        add(
            key = "Bulut Ayarları",
            intro = "Proje ve parametrelerin bulut senkronu.",
            steps = listOf("Sunucu & kimlik bilgisi gir", "Yükle / indir işlemi", "Çakışmayı tarih ile çöz"),
            warnings = listOf("Yanlış projeyi üzerine yazma"))
        add(
            key = "Yazılım Ayarları",
            intro = "Genel arayüz, doğruluk limitleri ve varsayılanlar.",
            steps = listOf("HRMS/VRMS limitlerini ayarla", "Varsayılan jalon yüksekliği belirle"))
        add(
            key = "Yazılım Güncelleme",
            intro = "Sürüm kontrolü ve güncelleme.",
            steps = listOf("Yeni sürüm sorgula", "İndir & doğrula", "Yeniden başlat"),
            tips = listOf("Ölçüm ortasında güncelleme yapma"))
        add(
            key = "Haberleşme",
            intro = "Cihaza BT / Wi‑Fi / Seri bağlantı.",
            steps = listOf("Üretici & model seç", "Modu seç ve tara", "Eşleş & bağlan"),
            warnings = listOf("Yanlış cihaza otomatik bağlanma"))
        add(
            key = "GNSS İzleme",
            intro = "Çözüm durumu, uydu sayısı, gecikme ve RMS izleme.",
            steps = listOf("Bağlantıyı doğrula", "Single→Float→Fix geçişini izle"),
            tips = listOf("Yüksek PDOP = kötü geometri"))
        add(
            key = "NTRIP Profilleri",
            intro = "NTRIP sunucu / mountpoint profilleri.",
            steps = listOf("Sunucu & port gir", "Kaynak tablosu al", "Mountpoint seç ve bağlan"))
        add(
            key = "Gezici Ayarları",
            intro = "Rover kesim açısı, aRTK, ham veri.",
            steps = listOf("Kesim açısı 10‑15°", "aRTK yaş sınırı ayarla", "Ham kayıt opsiyonel"))
        add(
            key = "Sabit Ayarları",
            intro = "Baz kimliği, başlangıç modu ve yayın parametreleri.",
            steps = listOf("Baz ID ver", "Bilinen / herhangi nokta seç", "Anten yüksekliğini gir"),
            warnings = listOf("Yanlış anten yüksekliği sistematik hata"))
        add(
            key = "Cihaz Bilgisi",
            intro = "Seri numarası, FW sürümü, batarya.",
            steps = listOf("Bağlandıktan sonra sorgula"))
        add(
            key = "Cihaz Kaydı",
            intro = "Lisans aktivasyon / transfer.",
            steps = listOf("Lisans anahtarını gir", "Gerekirse çevrimdışı kod üret"))
        add(
            key = "Manyetik Tarama",
            intro = "Manyetik anomali / metal tespiti (opsiyonel).",
            devNote = "Sensör desteği erken aşama")
        add(
            key = "Nokta Alımı",
            intro = "RTK nokta kaydı.",
            steps = listOf("Limitleri kontrol et", "İsim artışını ayarla", "Kaydet ve kaliteyi doğrula"))
        add(
            key = "Detay Alımı",
            intro = "Kod sözlüğü ile detay toplama.",
            steps = listOf("Kod seç", "Layer görünürlüğünü optimize et"))
        add(
            key = "Nokta Aplikasyonu",
            intro = "Hedef noktaya yönelim ve tolerans kontrolü.",
            steps = listOf("Hedef seç", "ΔN/ΔE izle", "Tolerans içinde onayla"))
        add(
            key = "Hat Aplikasyonu",
            intro = "Polyline / hat aplikasyonu.",
            steps = listOf("Hatı içe aktar", "Zinciraj + ofset gir"))
        add(
            key = "CAD",
            intro = "DXF içe/dışa aktarım ve çizim.",
            steps = listOf("Layer seç", "DXF yükle", "Çizim objesi ekle"))
        add(
            key = "AR Aplikasyon",
            intro = "Kamera bindirme ile AR aplikasyon.",
            devNote = "Doğruluk testleri sınırlı")
        add(
            key = "Fotogrametri",
            intro = "Foto toplama & GCP stratejisi.",
            steps = listOf("Rota planla", "Sabit yükseklik kullan", "GCP doğrula"))
        add(
            key = "Statik Ölçüm",
            intro = "Uzun gözlem ham veri kaydı.",
            steps = listOf("PDOP uygun zaman seç", "Aralık & anten yüksekliği", "Kaydı başlat"))
        add(
            key = "Epoch Ölçüm",
            intro = "Epok/süre kontrollü otomatik kayıt.",
            steps = listOf("Epok sayısı belirle", "Limit aşımlarını izle"))
        add(
            key = "Yol Aplikasyonu",
            intro = "Yol ekseni & kesit aplikasyonu.",
            steps = listOf("Yol tanımı içe aktar", "İstasyon+ofset gir"))
        add(
            key = "Lokalizasyon",
            intro = "Yerel dönüşüm (Affine/benzerlik).",
            steps = listOf("Kalibrasyon noktaları ekle", "Hataları incele", "Parametreleri uygula"))
        add(
            key = "Koordinat Dönüşümü",
            intro = "Datum / projeksiyon dönüşümü tek nokta veya dosya.",
            steps = listOf("Kaynak & hedef seç", "Veriyi gir", "Dışa aktar"))
        add(
            key = "Açı Dönüştürme",
            intro = "DMS ↔ derece ↔ grad dönüşümleri.",
            steps = listOf("Format seç", "Değer gir") )
        add(
            key = "Hesaplamalar (COGO)",
            intro = "Klasik geodezik hesap araçları.",
            steps = listOf("İşlem türünü seç", "Parametreleri gir", "Sonucu kaydet"))
        add(
            key = "Veri Paylaşımı",
            intro = "Noktalar / dosyalar arası cihaz paylaşımı.",
            steps = listOf("Yöntem seç", "Öğeleri işaretle", "Paylaş"))
        add(
            key = "Alan Hesabı",
            intro = "Çokgen çevre ve alan.",
            steps = listOf("Noktaları sırala", "Çokgeni kapat", "Raporla"))
        add(
            key = "Uzaklık Hesabı",
            intro = "İki nokta / zincir toplamı / 3B mesafe.",
            steps = listOf("Kaynak & hedef seç", "Sonuçları değerlendir"))
        add(
            key = "Hacim Hesaplama",
            intro = "TIN / grid tabanlı hacim.",
            steps = listOf("Referans yüzey seç", "Noktaları/çokgeni belirle", "Hesapla"))
        add(
            key = "Hesap Makinesi",
            intro = "Temel bilimsel hesaplama yardımcı aracı.")
        add(
            key = "Crash Log",
            intro = "Son çökme istif izlerini görüntüle & raporla.",
            steps = listOf("Kaydı aç", "Stack trace kopyala", "Geri bildirim gönder"))
    }

    fun get(title: String): HelpContent? = map[title]
}
