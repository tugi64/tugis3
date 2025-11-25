package com.example.tugis3.ui.survey

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
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsCrosshairActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                SettingsCrosshairScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCrosshairScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    // Çizim Katmanı Ayarları
    var showPointLabels by remember { mutableStateOf(true) }
    var showLineLabels by remember { mutableStateOf(true) }
    var pointSize by remember { mutableStateOf(10f) }
    var lineThickness by remember { mutableStateOf(2f) }
    var pointColor by remember { mutableStateOf("Kırmızı") }
    var lineColor by remember { mutableStateOf("Mavi") }

    // Arka Plan Katmanı Ayarları
    var showSatelliteMap by remember { mutableStateOf(false) }
    var mapOpacity by remember { mutableStateOf(0.7f) }
    var showGrid by remember { mutableStateOf(true) }
    var gridSpacing by remember { mutableStateOf(100f) }

    // Nişangah Ayarları
    var crosshairSize by remember { mutableStateOf(50f) }
    var crosshairColor by remember { mutableStateOf("Kırmızı") }
    var crosshairThickness by remember { mutableStateOf(2f) }
    var crosshairStyle by remember { mutableStateOf("Çarpı") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Katman Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Sıfırla */ }) {
                        Icon(Icons.Default.Refresh, "Varsayılana Dön")
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Çizim Katmanı") },
                    icon = { Icon(Icons.Default.Layers, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Arka Plan") },
                    icon = { Icon(Icons.Default.Map, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Nişangah") },
                    icon = { Icon(Icons.Default.CenterFocusStrong, null) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Çizim Katmanı Ayarları
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Nokta Görünümü", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Nokta Etiketlerini Göster")
                                        Switch(checked = showPointLabels, onCheckedChange = { showPointLabels = it })
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Nokta Boyutu: ${pointSize.toInt()} px")
                                    Slider(
                                        value = pointSize,
                                        onValueChange = { pointSize = it },
                                        valueRange = 5f..30f,
                                        steps = 24
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Nokta Rengi")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Kırmızı", "Mavi", "Yeşil", "Sarı", "Turuncu").forEach { color ->
                                            FilterChip(
                                                selected = pointColor == color,
                                                onClick = { pointColor = color },
                                                label = { Text(color) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Çizgi Görünümü", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Çizgi Etiketlerini Göster")
                                        Switch(checked = showLineLabels, onCheckedChange = { showLineLabels = it })
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Çizgi Kalınlığı: ${lineThickness.toInt()} px")
                                    Slider(
                                        value = lineThickness,
                                        onValueChange = { lineThickness = it },
                                        valueRange = 1f..10f,
                                        steps = 8
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Çizgi Rengi")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Mavi", "Kırmızı", "Yeşil", "Siyah", "Beyaz").forEach { color ->
                                            FilterChip(
                                                selected = lineColor == color,
                                                onClick = { lineColor = color },
                                                label = { Text(color) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Arka Plan Katmanı Ayarları
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Harita Katmanı", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Uydu Haritası Göster")
                                        Switch(checked = showSatelliteMap, onCheckedChange = { showSatelliteMap = it })
                                    }

                                    if (showSatelliteMap) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Harita Şeffaflığı: ${(mapOpacity * 100).toInt()}%")
                                        Slider(
                                            value = mapOpacity,
                                            onValueChange = { mapOpacity = it },
                                            valueRange = 0f..1f
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Grid Ayarları", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Grid Çizgilerini Göster")
                                        Switch(checked = showGrid, onCheckedChange = { showGrid = it })
                                    }

                                    if (showGrid) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Grid Aralığı: ${gridSpacing.toInt()} m")
                                        Slider(
                                            value = gridSpacing,
                                            onValueChange = { gridSpacing = it },
                                            valueRange = 10f..500f,
                                            steps = 48
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, "Bilgi", modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "KML/KMZ dosyaları ve Google Maps katmanları Proje > Dosya Yöneticisi'nden içe aktarılabilir.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Nişangah Ayarları
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Nişangah Boyutu", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Slider(
                                        value = crosshairSize,
                                        onValueChange = { crosshairSize = it },
                                        valueRange = 20f..100f,
                                        steps = 15
                                    )
                                    Text("${crosshairSize.toInt()} px", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Çizgi Kalınlığı", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Slider(
                                        value = crosshairThickness,
                                        onValueChange = { crosshairThickness = it },
                                        valueRange = 1f..5f,
                                        steps = 3
                                    )
                                    Text("${crosshairThickness.toInt()} px", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Nişangah Stili", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    listOf("Çarpı", "Artı", "Daire", "Kare", "Hedef").forEach { style ->
                                        FilterChip(
                                            selected = crosshairStyle == style,
                                            onClick = { crosshairStyle = style },
                                            label = { Text(style) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Renk Seçimi", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Kırmızı", "Mavi", "Yeşil", "Sarı", "Beyaz").forEach { color ->
                                            FilterChip(
                                                selected = crosshairColor == color,
                                                onClick = { crosshairColor = color },
                                                label = { Text(color) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Önizleme */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Visibility, "Önizleme")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Önizleme")
                        }
                        Button(
                            onClick = { /* Kaydet */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, "Kaydet")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }
}
