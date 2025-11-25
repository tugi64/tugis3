@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.device

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                DeviceSettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tugis_settings", Context.MODE_PRIVATE) }

    // Dialog states
    var showHrmsDialog by remember { mutableStateOf(false) }
    var showVrmsDialog by remember { mutableStateOf(false) }
    var showPdopDialog by remember { mutableStateOf(false) }
    var showGecikmeDialog by remember { mutableStateOf(false) }
    var showNoktaAdiDialog by remember { mutableStateOf(false) }
    var showNoktaArtisiDialog by remember { mutableStateOf(false) }
    var showVarsayilanKodDialog by remember { mutableStateOf(false) }
    var showOturumSuresiDialog by remember { mutableStateOf(false) }
    var showAlimModuDialog by remember { mutableStateOf(false) }
    var showDataTransferDialog by remember { mutableStateOf(false) }

    // Alım Modları
    var selectedAlimModu by remember { mutableStateOf(sharedPrefs.getString("alim_modu", "Standart") ?: "Standart") }
    val alimModlari = listOf("Standart", "Epoklu", "Hızlı", "Otomatik")

    // Öğesi Görünürlük state'leri - SharedPreferences'tan yükle
    var egimOlcme by remember { mutableStateOf(sharedPrefs.getBoolean("egim_olcme", true)) }
    var tamHarita by remember { mutableStateOf(sharedPrefs.getBoolean("tam_harita", false)) }
    var haritayiDegistir by remember { mutableStateOf(sharedPrefs.getBoolean("haritayi_degistir", true)) }
    var antanAyarlari by remember { mutableStateOf(sharedPrefs.getBoolean("antan_ayarlari", false)) }
    var ekranOlcum by remember { mutableStateOf(sharedPrefs.getBoolean("ekran_olcum", true)) }
    var haritayiOtomatik by remember { mutableStateOf(sharedPrefs.getBoolean("haritayi_otomatik", false)) }
    var otomatikOlcekleme by remember { mutableStateOf(sharedPrefs.getBoolean("otomatik_olcekleme", true)) }

    // Seçenekler state'leri - SharedPreferences'tan yükle
    var mevcutKonum by remember { mutableStateOf(sharedPrefs.getBoolean("mevcut_konum", true)) }
    var ekranUzerindenOlcum by remember { mutableStateOf(sharedPrefs.getBoolean("ekran_uzerinden_olcum", false)) }
    var cadYazi by remember { mutableStateOf(sharedPrefs.getBoolean("cad_yazi", true)) }
    var koordinatDonustur by remember { mutableStateOf(sharedPrefs.getBoolean("koordinat_donustur", false)) }
    var cizmeVeAlan by remember { mutableStateOf(sharedPrefs.getBoolean("cizme_ve_alan", true)) }
    var statikKayit by remember { mutableStateOf(sharedPrefs.getBoolean("statik_kayit", false)) }
    var konumuGuncelle by remember { mutableStateOf(sharedPrefs.getBoolean("konumu_guncelle", true)) }
    var hesapMakinesi by remember { mutableStateOf(sharedPrefs.getBoolean("hesap_makinesi", false)) }
    var tarsKoordinat by remember { mutableStateOf(sharedPrefs.getBoolean("tars_koordinat", true)) }
    var hatVeAcili by remember { mutableStateOf(sharedPrefs.getBoolean("hat_ve_acili", false)) }
    var vektor by remember { mutableStateOf(sharedPrefs.getBoolean("vektor", true)) }

    // Çözüm Durumu değerleri - SharedPreferences'tan yükle
    var hrmsLimit by remember { mutableStateOf(sharedPrefs.getFloat("hrms_limit", 0.05f)) }
    var vrmsLimit by remember { mutableStateOf(sharedPrefs.getFloat("vrms_limit", 0.1f)) }
    var pdopLimit by remember { mutableStateOf(sharedPrefs.getFloat("pdop_limit", 3.0f)) }
    var gecikmeLimit by remember { mutableStateOf(sharedPrefs.getInt("gecikme_limit", 5)) }
    var ayniIslmKullanimi by remember { mutableStateOf(sharedPrefs.getBoolean("ayni_islm_kullanimi", false)) }
    var girdilenSonNoktaya by remember { mutableStateOf(sharedPrefs.getBoolean("girdilen_son_noktaya", true)) }
    var noktaAdi by remember { mutableStateOf(sharedPrefs.getString("nokta_adi", "P1") ?: "P1") }
    var noktaAdiArtisi by remember { mutableStateOf(sharedPrefs.getInt("nokta_adi_artisi", 1)) }
    var varsayilanKod by remember { mutableStateOf(sharedPrefs.getString("varsayilan_kod", "Son Nokta ile Ayni") ?: "Son Nokta ile Ayni") }
    var oturumSuresi by remember { mutableStateOf(sharedPrefs.getInt("oturum_suresi", 3)) }

    // Veri Transfer Ayarları
    var yardimciNokta by remember { mutableStateOf(sharedPrefs.getBoolean("yardimci_nokta", true)) }
    var olcumNoktasi by remember { mutableStateOf(sharedPrefs.getBoolean("olcum_noktasi", true)) }
    var epoklu by remember { mutableStateOf(sharedPrefs.getBoolean("epoklu", true)) }
    var girisNoktasi by remember { mutableStateOf(sharedPrefs.getBoolean("giris_noktasi", true)) }
    var noktaHesapla by remember { mutableStateOf(sharedPrefs.getBoolean("nokta_hesapla", true)) }
    var aplikasyonNoktasi by remember { mutableStateOf(sharedPrefs.getBoolean("aplikasyon_noktasi", true)) }
    var ekranNoktasi by remember { mutableStateOf(sharedPrefs.getBoolean("ekran_noktasi", true)) }
    var bazNoktasi by remember { mutableStateOf(sharedPrefs.getBoolean("baz_noktasi", true)) }

    // Alım Modu Spesifik Ayarları
    var epokSayisi by remember { mutableStateOf(sharedPrefs.getInt("epok_sayisi", 10)) }
    var epokAraligiSaniye by remember { mutableStateOf(sharedPrefs.getInt("epok_araligi", 1)) }
    var hizliModPencereGenisligi by remember { mutableStateOf(sharedPrefs.getInt("hizli_mod_pencere", 5)) }
    var otomatikModMesafe by remember { mutableStateOf(sharedPrefs.getFloat("otomatik_mod_mesafe", 1.0f)) }

    // İşlevsel fonksiyonlar
    fun saveSettings() {
        with(sharedPrefs.edit()) {
            // Alım Modu
            putString("alim_modu", selectedAlimModu)

            // Öğesi Görünürlük
            putBoolean("egim_olcme", egimOlcme)
            putBoolean("tam_harita", tamHarita)
            putBoolean("haritayi_degistir", haritayiDegistir)
            putBoolean("antan_ayarlari", antanAyarlari)
            putBoolean("ekran_olcum", ekranOlcum)
            putBoolean("haritayi_otomatik", haritayiOtomatik)
            putBoolean("otomatik_olcekleme", otomatikOlcekleme)

            // Seçenekler
            putBoolean("mevcut_konum", mevcutKonum)
            putBoolean("ekran_uzerinden_olcum", ekranUzerindenOlcum)
            putBoolean("cad_yazi", cadYazi)
            putBoolean("koordinat_donustur", koordinatDonustur)
            putBoolean("cizme_ve_alan", cizmeVeAlan)
            putBoolean("statik_kayit", statikKayit)
            putBoolean("konumu_guncelle", konumuGuncelle)
            putBoolean("hesap_makinesi", hesapMakinesi)
            putBoolean("tars_koordinat", tarsKoordinat)
            putBoolean("hat_ve_acili", hatVeAcili)
            putBoolean("vektor", vektor)

            // Çözüm Durumu
            putFloat("hrms_limit", hrmsLimit)
            putFloat("vrms_limit", vrmsLimit)
            putFloat("pdop_limit", pdopLimit)
            putInt("gecikme_limit", gecikmeLimit)
            putBoolean("ayni_islm_kullanimi", ayniIslmKullanimi)
            putBoolean("girdilen_son_noktaya", girdilenSonNoktaya)
            putString("nokta_adi", noktaAdi)
            putInt("nokta_adi_artisi", noktaAdiArtisi)
            putString("varsayilan_kod", varsayilanKod)
            putInt("oturum_suresi", oturumSuresi)

            // Veri Transfer
            putBoolean("yardimci_nokta", yardimciNokta)
            putBoolean("olcum_noktasi", olcumNoktasi)
            putBoolean("epoklu", epoklu)
            putBoolean("giris_noktasi", girisNoktasi)
            putBoolean("nokta_hesapla", noktaHesapla)
            putBoolean("aplikasyon_noktasi", aplikasyonNoktasi)
            putBoolean("ekran_noktasi", ekranNoktasi)
            putBoolean("baz_noktasi", bazNoktasi)

            // Alım Modu Spesifik
            putInt("epok_sayisi", epokSayisi)
            putInt("epok_araligi", epokAraligiSaniye)
            putInt("hizli_mod_pencere", hizliModPencereGenisligi)
            putFloat("otomatik_mod_mesafe", otomatikModMesafe)

            apply()
        }
        Toast.makeText(context, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
    }

    fun resetToDefaults() {
        selectedAlimModu = "Standart"
        egimOlcme = true
        tamHarita = false
        haritayiDegistir = true
        antanAyarlari = false
        ekranOlcum = true
        haritayiOtomatik = false
        otomatikOlcekleme = true

        mevcutKonum = true
        ekranUzerindenOlcum = false
        cadYazi = true
        koordinatDonustur = false
        cizmeVeAlan = true
        statikKayit = false
        konumuGuncelle = true
        hesapMakinesi = false
        tarsKoordinat = true
        hatVeAcili = false
        vektor = true

        hrmsLimit = 0.05f
        vrmsLimit = 0.1f
        pdopLimit = 3.0f
        gecikmeLimit = 5
        ayniIslmKullanimi = false
        girdilenSonNoktaya = true
        noktaAdi = "P1"
        noktaAdiArtisi = 1
        varsayilanKod = "Son Nokta ile Ayni"
        oturumSuresi = 3

        yardimciNokta = true
        olcumNoktasi = true
        epoklu = true
        girisNoktasi = true
        noktaHesapla = true
        aplikasyonNoktasi = true
        ekranNoktasi = true
        bazNoktasi = true

        epokSayisi = 10
        epokAraligiSaniye = 1
        hizliModPencereGenisligi = 5
        otomatikModMesafe = 1.0f

        Toast.makeText(context, "Varsayılan ayarlar yüklendi", Toast.LENGTH_SHORT).show()
    }

    fun openPhotoGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        try {
            context.startActivity(intent)
            Toast.makeText(context, "Fotoğraf galerisi açıldı", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Galeri açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun performMeasurement() {
        when(selectedAlimModu) {
            "Standart" -> {
                Toast.makeText(context, "Standart ölçüm başlatıldı - Koordinat değerleri, hata oranları gösterilir", Toast.LENGTH_LONG).show()
            }
            "Epoklu" -> {
                Toast.makeText(context, "Epoklu ölçüm başlatıldı - $epokSayisi epok, ${epokAraligiSaniye}s aralık", Toast.LENGTH_LONG).show()
            }
            "Hızlı" -> {
                Toast.makeText(context, "Hızlı ölçüm başlatıldı - ${hizliModPencereGenisligi}s pencere genişliği", Toast.LENGTH_LONG).show()
            }
            "Otomatik" -> {
                Toast.makeText(context, "Otomatik ölçüm başlatıldı - ${otomatikModMesafe}m mesafe kriteri", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ekran Bilgisi ve Alım Modları") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Alım Modu Seçimi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Alım Modu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAlimModuDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Seçili Mod")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedAlimModu,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .background(
                                        when(selectedAlimModu) {
                                            "Standart" -> Color.Blue.copy(alpha = 0.1f)
                                            "Epoklu" -> Color.Green.copy(alpha = 0.1f)
                                            "Hızlı" -> Color(0xFFFF9800).copy(alpha = 0.1f) // Orange
                                            "Otomatik" -> Color(0xFF9C27B0).copy(alpha = 0.1f) // Purple
                                            else -> Color.Gray.copy(alpha = 0.1f)
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    // Alım Modu Açıklaması
                    Text(
                        text = when(selectedAlimModu) {
                            "Standart" -> "Nokta kaydından önce koordinat değerleri, hata oranları gösterilir. Kayıt işlemi sonlandırılır."
                            "Epoklu" -> "Koordinat değerleri gösterilir. Her epoktaki sapmalar grafik olarak gösterilir. $epokSayisi epok okunacak."
                            "Hızlı" -> "Herhangi bir pencere değişimi olmaz. Nokta ölçüsü ve kayıt yapılır."
                            "Otomatik" -> "Mesafe kriteri ayarı için pencere açılmaz. Ölçü sırasında belirlenen kriterlere göre alım işlemi başlar."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Alım Modu Spesifik Ayarlar
                    when(selectedAlimModu) {
                        "Epoklu" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Epok sayısı dialog */ },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Epok Sayısı")
                                Text(epokSayisi.toString(), fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Epok aralığı dialog */ },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Epok Aralığı (saniye)")
                                Text("${epokAraligiSaniye}s", fontWeight = FontWeight.Medium)
                            }
                        }
                        "Hızlı" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Pencere genişliği dialog */ },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pencere Genişliği")
                                Text("${hizliModPencereGenisligi}s", fontWeight = FontWeight.Medium)
                            }
                        }
                        "Otomatik" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Mesafe kriteri dialog */ },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mesafe Kriteri")
                                Text("${otomatikModMesafe}m", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Ölçüm ve Fotoğraf Butonları
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { performMeasurement() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Ölçüm Başlat")
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = { openPhotoGallery() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Fotoğraf")
                        }
                    }
                }
            }

            // Veri Transfer Ayarları
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDataTransferDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Veri Transfer Ayarları",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }

                    Text(
                        "Nokta verilerinin dışa aktarımı için seçenekleri yapılandırın",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Öğesi Görünürlük
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Öğesi Görünürlük",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SettingsRow("Eğim Ölçme", egimOlcme) {
                        egimOlcme = it
                        Toast.makeText(context, "Eğim Ölçme ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Tam Harita", tamHarita) {
                        tamHarita = it
                        Toast.makeText(context, "Tam Harita ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Haritayı Değiştir", haritayiDegistir) {
                        haritayiDegistir = it
                        Toast.makeText(context, "Haritayı Değiştir ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Antan Ayarları", antanAyarlari) {
                        antanAyarlari = it
                        Toast.makeText(context, "Antan Ayarları ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Ekran Ölçüm", ekranOlcum) {
                        ekranOlcum = it
                        Toast.makeText(context, "Ekran Ölçüm ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Haritayı Otomatik", haritayiOtomatik) {
                        haritayiOtomatik = it
                        Toast.makeText(context, "Haritayı Otomatik ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Otomatik Ölçekleme", otomatikOlcekleme) {
                        otomatikOlcekleme = it
                        Toast.makeText(context, "Otomatik Ölçekleme ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Seçenekler
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Seçenekler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SettingsRow("Mevcut Konum", mevcutKonum) {
                        mevcutKonum = it
                        Toast.makeText(context, "Mevcut Konum ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Ekran Üzerinden Ölçüm", ekranUzerindenOlcum) {
                        ekranUzerindenOlcum = it
                        Toast.makeText(context, "Ekran Üzerinden Ölçüm ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("CAD Yazı", cadYazi) {
                        cadYazi = it
                        Toast.makeText(context, "CAD Yazı ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Koordinat Dönüştür", koordinatDonustur) {
                        koordinatDonustur = it
                        Toast.makeText(context, "Koordinat Dönüştür ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Çizme Ve Alan", cizmeVeAlan) {
                        cizmeVeAlan = it
                        Toast.makeText(context, "Çizme Ve Alan ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Statik Kayıt", statikKayit) {
                        statikKayit = it
                        Toast.makeText(context, "Statik Kayıt ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Konumu Güncelle", konumuGuncelle) {
                        konumuGuncelle = it
                        Toast.makeText(context, "Konumu Güncelle ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Hesap Makinesi", hesapMakinesi) {
                        hesapMakinesi = it
                        Toast.makeText(context, "Hesap Makinesi ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Tars Koordinat", tarsKoordinat) {
                        tarsKoordinat = it
                        Toast.makeText(context, "Tars Koordinat ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Hat ve Açılı Koordinat", hatVeAcili) {
                        hatVeAcili = it
                        Toast.makeText(context, "Hat ve Açılı Koordinat ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Vektör", vektor) {
                        vektor = it
                        Toast.makeText(context, "Vektör ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Çözüm Durumu
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Çözüm Durumu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // HRMS Limit - Tıklanabilir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHrmsDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HRMS Limit")
                        Text(
                            text = String.format("%.2f", hrmsLimit),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // VRMS Limit - Tıklanabilir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVrmsDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VRMS Limit")
                        Text(
                            text = String.format("%.1f", vrmsLimit),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // PDOP Limit - Tıklanabilir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPdopDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PDOP Limit")
                        Text(
                            text = String.format("%.1f", pdopLimit),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Gecikme Limiti - Tıklanabilir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGecikmeDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gecikme Limiti")
                        Text(
                            text = gecikmeLimit.toString(),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    SettingsRow("Aynı İşlm Kullanımı", ayniIslmKullanimi) {
                        ayniIslmKullanimi = it
                        Toast.makeText(context, "Aynı İşlm Kullanımı ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }
                    SettingsRow("Girdilen Son Noktaya Odaklan", girdilenSonNoktaya) {
                        girdilenSonNoktaya = it
                        Toast.makeText(context, "Girdilen Son Noktaya Odaklan ${if(it) "açıldı" else "kapandı"}", Toast.LENGTH_SHORT).show()
                    }

                    // Nokta Adı - Tıklanabilir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNoktaAdiDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nokta Adı")
                        Text(
                            text = noktaAdi,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Nokta Adı Artışı - Tıklanabilir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNoktaArtisiDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nokta Adı Artışı")
                        Text(
                            text = noktaAdiArtisi.toString(),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Varsayılan Kod - Tıklanabilir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVarsayilanKodDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Varsayılan Kod")
                        Text(
                            text = varsayilanKod,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }

                    // Oturum Süresi - Tıklanabilir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOturumSuresiDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Oturum Süresi")
                        Text(
                            text = oturumSuresi.toString(),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Alt butonlar - Artık işlevsel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { resetToDefaults() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("VARSAYILAN")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        saveSettings()
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("TAMAM")
                }
            }
        }
    }

    // Alım Modu Seçim Dialog'u
    if (showAlimModuDialog) {
        Dialog(onDismissRequest = { showAlimModuDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Alım Modu Seçin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    alimModlari.forEach { mod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAlimModu = mod
                                    showAlimModuDialog = false
                                    Toast.makeText(context, "$mod modu seçildi", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAlimModu == mod,
                                onClick = {
                                    selectedAlimModu = mod
                                    showAlimModuDialog = false
                                    Toast.makeText(context, "$mod modu seçildi", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mod,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when(mod) {
                                        "Standart" -> "Detaylı koordinat gösterimi"
                                        "Epoklu" -> "Çoklu ölçüm ve grafik gösterim"
                                        "Hızlı" -> "Hızlı ölçüm ve kayıt"
                                        "Otomatik" -> "Mesafe kriteri ile otomatik ölçüm"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { showAlimModuDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("İptal")
                    }
                }
            }
        }
    }

    // Veri Transfer Dialog'u
    if (showDataTransferDialog) {
        Dialog(onDismissRequest = { showDataTransferDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Veri Transfer Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SettingsRow("Yardımcı Nokta", yardimciNokta) { yardimciNokta = it }
                    SettingsRow("Ölçüm Noktası", olcumNoktasi) { olcumNoktasi = it }
                    SettingsRow("Epoklu", epoklu) { epoklu = it }
                    SettingsRow("Giriş Noktası", girisNoktasi) { girisNoktasi = it }
                    SettingsRow("Nokta Hesapla", noktaHesapla) { noktaHesapla = it }
                    SettingsRow("Aplikasyon Noktası", aplikasyonNoktasi) { aplikasyonNoktasi = it }
                    SettingsRow("Ekran Noktası", ekranNoktasi) { ekranNoktasi = it }
                    SettingsRow("Baz Noktası", bazNoktasi) { bazNoktasi = it }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { showDataTransferDialog = false }) {
                            Text("İptal")
                        }
                        TextButton(onClick = {
                            showDataTransferDialog = false
                            Toast.makeText(context, "Veri transfer ayarları güncellendi", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Tamam")
                        }
                    }
                }
            }
        }
    }

    // HRMS Dialog
    if (showHrmsDialog) {
        NumberInputDialog(
            title = "HRMS Limit",
            currentValue = hrmsLimit.toString(),
            onValueChange = { newValue ->
                newValue.toFloatOrNull()?.let { hrmsLimit = it }
                showHrmsDialog = false
            },
            onDismiss = { showHrmsDialog = false }
        )
    }

    if (showVrmsDialog) {
        NumberInputDialog(
            title = "VRMS Limit",
            currentValue = vrmsLimit.toString(),
            onValueChange = { newValue ->
                newValue.toFloatOrNull()?.let { vrmsLimit = it }
                showVrmsDialog = false
            },
            onDismiss = { showVrmsDialog = false }
        )
    }

    if (showPdopDialog) {
        NumberInputDialog(
            title = "PDOP Limit",
            currentValue = pdopLimit.toString(),
            onValueChange = { newValue ->
                newValue.toFloatOrNull()?.let { pdopLimit = it }
                showPdopDialog = false
            },
            onDismiss = { showPdopDialog = false }
        )
    }

    if (showGecikmeDialog) {
        NumberInputDialog(
            title = "Gecikme Limiti",
            currentValue = gecikmeLimit.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { gecikmeLimit = it }
                showGecikmeDialog = false
            },
            onDismiss = { showGecikmeDialog = false }
        )
    }

    if (showNoktaAdiDialog) {
        TextInputDialog(
            title = "Nokta Adı",
            currentValue = noktaAdi,
            onValueChange = { newValue ->
                noktaAdi = newValue
                showNoktaAdiDialog = false
            },
            onDismiss = { showNoktaAdiDialog = false }
        )
    }

    if (showNoktaArtisiDialog) {
        NumberInputDialog(
            title = "Nokta Adı Artışı",
            currentValue = noktaAdiArtisi.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { noktaAdiArtisi = it }
                showNoktaArtisiDialog = false
            },
            onDismiss = { showNoktaArtisiDialog = false }
        )
    }

    if (showVarsayilanKodDialog) {
        TextInputDialog(
            title = "Varsayılan Kod",
            currentValue = varsayilanKod,
            onValueChange = { newValue ->
                varsayilanKod = newValue
                showVarsayilanKodDialog = false
            },
            onDismiss = { showVarsayilanKodDialog = false }
        )
    }

    if (showOturumSuresiDialog) {
        NumberInputDialog(
            title = "Oturum Süresi",
            currentValue = oturumSuresi.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { oturumSuresi = it }
                showOturumSuresiDialog = false
            },
            onDismiss = { showOturumSuresiDialog = false }
        )
    }
}

@Composable
private fun SettingsRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun NumberInputDialog(
    title: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Değer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                    TextButton(onClick = { onValueChange(textValue) }) {
                        Text("Tamam")
                    }
                }
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Değer") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                    TextButton(onClick = { onValueChange(textValue) }) {
                        Text("Tamam")
                    }
                }
            }
        }
    }
}
