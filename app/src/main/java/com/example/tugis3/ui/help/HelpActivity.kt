package com.example.tugis3.ui.help

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HelpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                HelpScreen(
                    onBack = { finish() },
                    onOpenUrl = { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    }
                )
            }
        }
    }
}

data class HelpSection(
    val title: String,
    val icon: ImageVector,
    val items: List<HelpItem>
)

data class HelpItem(
    val title: String,
    val description: String,
    val url: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    // Sekmeler: Konular / Kılavuzlar
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yardım ve Dokümantasyon") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Konular") }, icon = { Icon(Icons.Filled.MenuBook, null) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Kılavuzlar") }, icon = { Icon(Icons.Filled.Description, null) })
            }

            when (selectedTab) {
                0 -> HelpTopics(onOpenUrl = onOpenUrl)
                1 -> ManualsViewer()
            }
        }
    }
}

@Composable
private fun HelpTopics(onOpenUrl: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    val helpSections = remember {
        listOf(
            HelpSection(
                title = "Başlangıç Kılavuzu",
                icon = Icons.Default.School,
                items = listOf(
                    HelpItem(
                        "İlk Proje Oluşturma",
                        "Yeni bir proje oluşturmak için Proje sekmesinden 'Yeni Proje' butonuna tıklayın. Proje adı, koordinat sistemi ve datum bilgilerini girin."
                    ),
                    HelpItem(
                        "Cihaz Bağlantısı",
                        "Bluetooth veya USB üzerinden GNSS cihazınızı bağlayın. Cihaz sekmesinden 'Cihaz Ara' seçeneğini kullanın."
                    ),
                    HelpItem(
                        "İlk Ölçüm Alma",
                        "Ölçüm sekmesinden nokta tipini seçin, nokta numarası girin ve 'Ölçüm Al' butonuna basın."
                    )
                )
            ),
            HelpSection(
                title = "Ölçüm İşlemleri",
                icon = Icons.Default.GpsFixed,
                items = listOf(
                    HelpItem(
                        "Nokta Ölçümü",
                        "Statik veya RTK modunda nokta ölçümü yapabilirsiniz. Ölçüm kalitesine dikkat edin."
                    ),
                    HelpItem(
                        "Stake Out (İşaretleme)",
                        "Önceden tanımlanmış noktalara navigasyon yaparak arazi üzerinde işaretleme yapın."
                    ),
                    HelpItem(
                        "Sürekli Ölçüm",
                        "Hareket halinde GPS track kaydı için sürekli ölçüm modunu kullanın."
                    ),
                    HelpItem(
                        "Ofset Ölçümü",
                        "Ulaşılamayan noktalara mesafe ve açı ile ofset ölçümü yapın."
                    )
                )
            ),
            HelpSection(
                title = "Koordinat Sistemleri",
                icon = Icons.Default.Public,
                items = listOf(
                    HelpItem(
                        "Projeksiyon Seçimi",
                        "TM3, UTM veya yerel koordinat sistemlerini seçebilirsiniz. Proje ayarlarından yapılandırın."
                    ),
                    HelpItem(
                        "Datum Dönüşümü",
                        "WGS84, ITRF, ED50 gibi farklı datum'lar arası dönüşüm yapın."
                    ),
                    HelpItem(
                        "Lokalizasyon",
                        "Yerel koordinat sistemine geçiş için en az 3 ortak nokta kullanarak lokalizasyon yapın."
                    )
                )
            ),
            HelpSection(
                title = "Veri Yönetimi",
                icon = Icons.Default.Storage,
                items = listOf(
                    HelpItem(
                        "Veri İçe/Dışa Aktarma",
                        "CSV, DXF, KML formatlarında veri aktarımı yapabilirsiniz."
                    ),
                    HelpItem(
                        "Yedekleme",
                        "Proje verilerinizi düzenli olarak yedekleyin. Ayarlar > Yedekle seçeneğini kullanın."
                    ),
                    HelpItem(
                        "Veri Senkronizasyonu",
                        "FTP veya bulut depolama ile veri senkronizasyonu yapın."
                    )
                )
            ),
            HelpSection(
                title = "CAD ve Harita",
                icon = Icons.Default.Draw,
                items = listOf(
                    HelpItem(
                        "CAD Çizim",
                        "Nokta, çizgi, poligon ve daire çizimi yapabilirsiniz."
                    ),
                    HelpItem(
                        "Katman Yönetimi",
                        "Farklı katmanlarda çalışarak verileri organize edin."
                    ),
                    HelpItem(
                        "Harita Görünümü",
                        "Ölçülen noktaları ve CAD objelerini harita üzerinde görüntüleyin."
                    )
                )
            ),
            HelpSection(
                title = "Hesaplama Araçları",
                icon = Icons.Default.Calculate,
                items = listOf(
                    HelpItem(
                        "COGO Hesaplamaları",
                        "Mesafe, açı, alan, çevre hesaplamaları yapın."
                    ),
                    HelpItem(
                        "Koordinat Dönüştürme",
                        "Farklı koordinat sistemleri arasında dönüşüm yapın."
                    ),
                    HelpItem(
                        "Hacim Hesaplama",
                        "Kesit veya grid metodu ile hacim hesabı yapın."
                    ),
                    HelpItem(
                        "Grid to Ground",
                        "Projeksiyon düzeltmeleri uygulayarak grid mesafeleri arazi mesafesine dönüştürün."
                    )
                )
            ),
            HelpSection(
                title = "Sorun Giderme",
                icon = Icons.Default.BugReport,
                items = listOf(
                    HelpItem(
                        "GPS Sinyali Zayıf",
                        "Açık alanda ölçüm yapın. En az 5 uydu görünür olmalı. PDOP değeri 3'ün altında olmalı."
                    ),
                    HelpItem(
                        "Bluetooth Bağlantı Sorunu",
                        "Cihazı yeniden eşleştirin. Android Bluetooth ayarlarından önce eşleştirilmelidir."
                    ),
                    HelpItem(
                        "Yüksek Hassasiyet Hatası",
                        "RTK fix bekleyin. Float çözümde hassasiyet düşüktür."
                    ),
                    HelpItem(
                        "Veri Kaydetme Hatası",
                        "Depolama izinlerini kontrol edin. Yeterli boş alan olduğundan emin olun."
                    )
                )
            ),
            HelpSection(
                title = "İletişim ve Destek",
                icon = Icons.AutoMirrored.Filled.ContactSupport,
                items = listOf(
                    HelpItem(
                        "Teknik Destek",
                        "Teknik destek için support@tugis.com adresine e-posta gönderin.",
                        "mailto:support@tugis.com"
                    ),
                    HelpItem(
                        "Video Eğitimler",
                        "YouTube kanalımızda detaylı video eğitimler bulabilirsiniz.",
                        "https://youtube.com"
                    ),
                    HelpItem(
                        "Kullanıcı Forumu",
                        "Diğer kullanıcılarla deneyim paylaşın ve sorularınıza yanıt bulun.",
                        "https://forum.tugis.com"
                    )
                )
            )
        )
    }

    val filteredSections = remember(searchQuery, helpSections) {
        if (searchQuery.isEmpty()) helpSections else helpSections.mapNotNull { section ->
            val filteredItems = section.items.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            }
            if (filteredItems.isNotEmpty()) section.copy(items = filteredItems) else null
        }
    }

    // Arama Çubuğu
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Yardım konularında ara...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, "Temizle") }
            }
        },
        singleLine = true
    )

    if (filteredSections.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Sonuç bulunamadı", style = MaterialTheme.typography.titleMedium)
            Text(
                "Farklı bir anahtar kelime deneyin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredSections.forEach { section ->
                item {
                    HelpSectionCard(
                        section = section,
                        isExpanded = section.title == "Başlangıç Kılavuzu", // varsayılanı açık tutabiliriz
                        onToggle = { /* no-op in simple render */ },
                        onItemClick = { item -> item.url?.let { onOpenUrl(it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualsViewer() {
    val context = LocalContext.current
    var selectedManual by remember { mutableStateOf("kullanim_klavuzu_tr.txt") }
    var searchQuery by remember { mutableStateOf("") }

    val manuals = remember {
        listOf(
            "kullanim_klavuzu_tr.txt" to "Türkçe Kılavuz",
            "user_guide_en_quick.txt" to "User Guide (Quick)",
            "user_guide_en_full.txt" to "User Guide (Full)"
        )
    }

    // İçerik yükleme
    val content by produceState(initialValue = "Yükleniyor...", selectedManual) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("manual/$selectedManual").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.getOrElse { "Kılavuz yüklenemedi: ${it.message}" }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Seçici ve arama
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Basit dropdown yerine üç butonlu seçim (bağımlılık eklemeden)
            manuals.forEach { (file, title) ->
                AssistChip(
                    onClick = { selectedManual = file },
                    label = { Text(title, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Description, null) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = AssistChipDefaults.assistChipColors()
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            placeholder = { Text("Kılavuz içinde ara...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

        // İçerik
        val filteredContent = remember(content, searchQuery) {
            if (searchQuery.isBlank()) content else content
                .lineSequence()
                .filter { it.contains(searchQuery, ignoreCase = true) }
                .joinToString(separator = "\n")
        }

        // move lines computation out of LazyColumn to avoid calling @Composable (remember) inside LazyColumn's lambda
        val lines = remember(filteredContent) { filteredContent.lines() }

        // Metin görüntüleyici
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                items(lines.size) { idx ->
                    Text(lines[idx])
                }
            }
        }
    }
}

@Composable
private fun HelpSectionCard(
    section: HelpSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onItemClick: (HelpItem) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Başlık
            ListItem(
                headlineContent = {
                    Text(
                        section.title,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingContent = {
                    Icon(section.icon, null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null
                    )
                },
                modifier = Modifier.clickable(onClick = onToggle)
            )

            // İçerik
            if (isExpanded) {
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(Modifier.height(8.dp))
                    section.items.forEach { item ->
                        HelpItemCard(item = item, onClick = { onItemClick(item) })
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun HelpItemCard(item: HelpItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.url != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.url != null) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
