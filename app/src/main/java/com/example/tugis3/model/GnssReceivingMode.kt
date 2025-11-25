package com.example.tugis3.model

/**
 * GNSS alım modları
 */
enum class GnssReceivingMode(
    val displayName: String,
    val description: String
) {
    STANDARD("Standart", "Nokta kayıttan önce koordinat değerleri, hata oranları gibi nokta detayları gözlenir"),
    EPOCH("Epoklu", "Ölçü sırasında oluşan hatalar grafik olarak gözlenir. 10 epok okunarak nokta hataları kaydedilir"),
    FAST("Hızlı", "Herhangi bir pencere değişimi olmaz. Nokta ölçüsü ve kaydı yapılır"),
    AUTOMATIC("Otomatik", "Belirlenen kriterlere göre alım işlemi okuma butonu ile başlar ve otomatik durur")
}

/**
 * GNSS cihaz ayarları
 */
data class GnssDeviceSettings(
    val hrmsLimit: Double = 0.05,           // HRMS limiti (m)
    val vrmsLimit: Double = 0.1,            // VRMS limiti (m)
    val pdopLimit: Double = 3.0,            // PDOP limiti
    val passLimit: Int = 5,                 // Geçkme limiti
    val useSameOperation: Boolean = false,   // Aynı işlem kullanımı
    val focusLastPoint: Boolean = true,      // Girilen son noktaya odaklan
    val pointName: String = "P11",          // Nokta adı
    val pointIncrement: Int = 1,            // Nokta adı artışı
    val defaultCode: String = "Son Nokta ile Aynı", // Varsayılan kod
    val sessionDuration: Int = 3,           // Oturum süresi (saniye)
    val epochCount: Int = 3,                // Epok sayısı
    val receivingMode: GnssReceivingMode = GnssReceivingMode.STANDARD, // Alım modu
    val imuEnabled: Boolean = false,        // IMU/Elektronik düzeç durumu
    val solutionStatus: String = "FIX",     // Çözüm durumu
    val antennaHeight: Double = 2.0         // Anten yüksekliği (m)
)

/**
 * CAD seçenekleri
 */
enum class CadOption(val displayName: String) {
    SQUARE("Kare"),
    LINE("Hat"),
    MULTI_LINE("Çoklu Çizgi"),
    POINT_PERPENDICULAR("Dikdörtgen"),
    PERPENDICULAR_MARKER("Dikdörtgen Markezi"),
    POLYGON("Çokgen"),
    CIRCLE_2P("Daire 2P"),
    CIRCLE_3P("Daire 3P"),
    ARC("Yay"),
    POINT("Nokta"),
    MULTI_POINT("Çoklu Eğri"),
    DEFAULT("Default")
}
