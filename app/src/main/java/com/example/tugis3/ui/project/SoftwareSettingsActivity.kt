package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SoftwareSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                SoftwareSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoftwareSettingsScreen(onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Genel", "Kayıt", "Sistem", "Görünüm")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Yazılım Ayarları") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> item { GeneralSettings(snackbar) }
                1 -> item { RecordSettings(snackbar) }
                2 -> item { SystemSettings(snackbar) }
                3 -> item { DisplaySettings(snackbar) }
            }
        }
    }
}

@Composable
private fun GeneralSettings(snackbar: SnackbarHostState) {
    var showShortcuts by remember { mutableStateOf(true) }
    var updateNotification by remember { mutableStateOf(true) }
    var voicePrompt by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Genel Ayarlar",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        SettingsCard {
            SettingsSwitchItem(
                title = "Kısayolları Göster",
                description = "Ana ekranda hızlı erişim butonları",
                checked = showShortcuts,
                onCheckedChange = { showShortcuts = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Güncelleme Bildirimi",
                description = "Yeni sürüm bildirimi al",
                checked = updateNotification,
                onCheckedChange = { updateNotification = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Sesli Uyarı",
                description = "İşlemler için ses çıkışı",
                checked = voicePrompt,
                onCheckedChange = { voicePrompt = it }
            )
        }
    }
}

@Composable
private fun RecordSettings(snackbar: SnackbarHostState) {
    var topoPoint by remember { mutableStateOf(true) }
    var controlPoint by remember { mutableStateOf(true) }
    var quickPoint by remember { mutableStateOf(true) }
    var autoPoint by remember { mutableStateOf(false) }
    var cornerPoint by remember { mutableStateOf(true) }
    var stopGoPoint by remember { mutableStateOf(true) }
    var tiltPoint by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Kayıt Ayarları",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Nokta türleri için kayıt seçenekleri",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingsCard {
            SettingsSwitchItem(
                title = "Topo Noktası",
                description = "Topoğrafik ölçüm noktaları",
                checked = topoPoint,
                onCheckedChange = { topoPoint = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Kontrol Noktası",
                description = "Referans kontrol noktaları",
                checked = controlPoint,
                onCheckedChange = { controlPoint = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Hızlı Nokta",
                description = "Hızlı ölçüm noktaları",
                checked = quickPoint,
                onCheckedChange = { quickPoint = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Otomatik Nokta",
                description = "Otomatik kayıt modu",
                checked = autoPoint,
                onCheckedChange = { autoPoint = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Köşe Noktası",
                description = "Köşe ve sınır noktaları",
                checked = cornerPoint,
                onCheckedChange = { cornerPoint = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Stop & Go Noktası",
                description = "Duraklı ölçüm noktaları",
                checked = stopGoPoint,
                onCheckedChange = { stopGoPoint = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Eğim Noktası",
                description = "Eğimli zemin düzeltmesi",
                checked = tiltPoint,
                onCheckedChange = { tiltPoint = it }
            )
        }
    }
}

@Composable
private fun SystemSettings(snackbar: SnackbarHostState) {
    var distanceUnit by remember { mutableStateOf("Metre") }
    var distanceDecimal by remember { mutableStateOf("3") }
    var angleFormat by remember { mutableStateOf("DMS") }
    var angleDecimal by remember { mutableStateOf("0") }
    var language by remember { mutableStateOf("Türkçe") }
    var autoBackup by remember { mutableStateOf(true) }
    var changeAlert by remember { mutableStateOf(true) }
    var locationServices by remember { mutableStateOf(true) }
    var autoStartBoot by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Sistem Ayarları",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Birim Ayarları
        Text(
            "Birim Ayarları",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        SettingsCard {
            SettingsDropdownItem(
                title = "Mesafe Birimi",
                value = distanceUnit,
                options = listOf("Metre", "Feet", "US Survey Feet"),
                onValueChange = { distanceUnit = it }
            )
            HorizontalDivider()
            SettingsDropdownItem(
                title = "Mesafe Ondalık",
                value = distanceDecimal,
                options = listOf("0", "1", "2", "3", "4"),
                onValueChange = { distanceDecimal = it }
            )
            HorizontalDivider()
            SettingsDropdownItem(
                title = "Açı Formatı",
                value = angleFormat,
                options = listOf("DMS", "Derece", "Gon", "Mil"),
                onValueChange = { angleFormat = it }
            )
            HorizontalDivider()
            SettingsDropdownItem(
                title = "Açı Ondalık",
                value = angleDecimal,
                options = listOf("0", "1", "2", "3", "4"),
                onValueChange = { angleDecimal = it }
            )
        }

        // Genel Sistem
        Text(
            "Genel Sistem",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        SettingsCard {
            SettingsDropdownItem(
                title = "Dil",
                value = language,
                options = listOf("Türkçe", "English", "中文", "Español"),
                onValueChange = { language = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Otomatik Yedekleme",
                description = "Ölçüm noktalarını otomatik yedekle",
                checked = autoBackup,
                onCheckedChange = { autoBackup = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Koordinat Değişikliği Uyarısı",
                description = "Base koordinat değişimi bildirimi",
                checked = changeAlert,
                onCheckedChange = { changeAlert = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Konum Servisleri",
                description = "GPS ve konum kullanımı",
                checked = locationServices,
                onCheckedChange = { locationServices = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Başlangıçta Otomatik Başlat",
                description = "Cihaz açıldığında uygulamayı başlat",
                checked = autoStartBoot,
                onCheckedChange = { autoStartBoot = it }
            )
        }
    }
}

@Composable
private fun DisplaySettings(snackbar: SnackbarHostState) {
    var cadBgColor by remember { mutableStateOf("Siyah") }
    var coordinateOrder by remember { mutableStateOf("N, E, H") }
    var displayType by remember { mutableStateOf("Liste") }
    var lastPoints by remember { mutableStateOf("50") }
    var screenOrientation by remember { mutableStateOf("Otomatik") }
    var fullScreen by remember { mutableStateOf(false) }
    var mapDisplay by remember { mutableStateOf(true) }
    var showPointName by remember { mutableStateOf(true) }
    var showCoordinates by remember { mutableStateOf(true) }
    var showSatellites by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Görünüm Ayarları",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // CAD Ayarları
        Text(
            "CAD Görünüm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        SettingsCard {
            SettingsDropdownItem(
                title = "CAD Arka Plan Rengi",
                value = cadBgColor,
                options = listOf("Siyah", "Beyaz", "Gri"),
                onValueChange = { cadBgColor = it }
            )
        }

        // Ekran Ayarları
        Text(
            "Ekran Ayarları",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        SettingsCard {
            SettingsDropdownItem(
                title = "Koordinat Sırası",
                value = coordinateOrder,
                options = listOf("N, E, H", "E, N, H", "X, Y, Z"),
                onValueChange = { coordinateOrder = it }
            )
            HorizontalDivider()
            SettingsDropdownItem(
                title = "Görünüm Tipi",
                value = displayType,
                options = listOf("Liste", "Harita", "İkisi Birden"),
                onValueChange = { displayType = it }
            )
            HorizontalDivider()
            SettingsDropdownItem(
                title = "Gösterilen Son Noktalar",
                value = lastPoints,
                options = listOf("10", "20", "50", "100", "Tümü"),
                onValueChange = { lastPoints = it }
            )
            HorizontalDivider()
            SettingsDropdownItem(
                title = "Ekran Yönü",
                value = screenOrientation,
                options = listOf("Otomatik", "Dikey", "Yatay"),
                onValueChange = { screenOrientation = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Tam Ekran Modu",
                description = "Durum çubuğunu gizle",
                checked = fullScreen,
                onCheckedChange = { fullScreen = it }
            )
        }

        // Gösterim Seçenekleri
        Text(
            "Gösterim Seçenekleri",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        SettingsCard {
            SettingsSwitchItem(
                title = "Harita Göster",
                description = "Harita görünümünü etkinleştir",
                checked = mapDisplay,
                onCheckedChange = { mapDisplay = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Nokta İsimlerini Göster",
                description = "Haritada nokta isimlerini göster",
                checked = showPointName,
                onCheckedChange = { showPointName = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Koordinatları Göster",
                description = "Haritada koordinatları göster",
                checked = showCoordinates,
                onCheckedChange = { showCoordinates = it }
            )
            HorizontalDivider()
            SettingsSwitchItem(
                title = "Uydu Bilgilerini Göster",
                description = "Uydu sayısı ve kalite göster",
                checked = showSatellites,
                onCheckedChange = { showSatellites = it }
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownItem(
    title: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

