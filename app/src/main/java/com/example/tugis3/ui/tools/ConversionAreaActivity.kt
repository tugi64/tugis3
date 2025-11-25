package com.example.tugis3.ui.tools

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
class ConversionAreaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                ConversionAreaScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversionAreaScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var pointList by remember { mutableStateOf("") }
    var calculatedArea by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var calculatedPerimeter by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedPoints by remember { mutableStateOf<List<String>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Çevre ve Alan Hesabı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Yardım */ }) {
                        Icon(Icons.Default.Help, "Yardım")
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
                    text = { Text("Alan Hesabı") },
                    icon = { Icon(Icons.Default.CropSquare, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Çevre Hesabı") },
                    icon = { Icon(Icons.Default.Timeline, null) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Alan Hesabı
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Nokta Seçimi", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        "Alan hesabı için en az 3 nokta seçilmelidir. Noktalar saat yönünde veya saat yönünün tersinde sırayla girilmelidir.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = pointList,
                                        onValueChange = { pointList = it },
                                        label = { Text("Nokta listesi") },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("P1, P2, P3, P4, ...") },
                                        supportingText = { Text("Noktaları virgülle ayırarak girin") },
                                        leadingIcon = { Icon(Icons.Default.Place, null) },
                                        minLines = 3
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { /* Haritadan seç */ },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Map, "Harita")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Haritadan Seç")
                                        }
                                        OutlinedButton(
                                            onClick = { /* Listeden seç */ },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.List, "Liste")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Listeden Seç")
                                        }
                                    }

                                    if (selectedPoints.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Seçili Noktalar: ${selectedPoints.size}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (calculatedArea.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, "Sonuç", tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Hesaplanan Alan", style = MaterialTheme.typography.titleMedium)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        ResultRow("Metrekare (m²)", calculatedArea["m2"] ?: "0.00")
                                        ResultRow("Kilometrekare (km²)", calculatedArea["km2"] ?: "0.00")
                                        ResultRow("Hektar (ha)", calculatedArea["ha"] ?: "0.00")
                                        ResultRow("Dönüm", calculatedArea["donum"] ?: "0.00")
                                        ResultRow("Acre", calculatedArea["acre"] ?: "0.00")
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Çevre Hesabı
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Nokta Seçimi", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        "Çevre hesabı için en az 2 nokta seçilmelidir. Kapalı poligon için son nokta otomatik olarak ilk noktaya bağlanır.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = pointList,
                                        onValueChange = { pointList = it },
                                        label = { Text("Nokta listesi") },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("P1, P2, P3, ...") },
                                        supportingText = { Text("Noktaları virgülle ayırarak girin") },
                                        leadingIcon = { Icon(Icons.Default.Timeline, null) },
                                        minLines = 3
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    var isClosedPolygon by remember { mutableStateOf(true) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Kapalı Poligon")
                                        Switch(
                                            checked = isClosedPolygon,
                                            onCheckedChange = { isClosedPolygon = it }
                                        )
                                    }
                                }
                            }
                        }

                        if (calculatedPerimeter.isNotEmpty()) {
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
                                            Icon(Icons.Default.CheckCircle, "Sonuç", tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Hesaplanan Çevre", style = MaterialTheme.typography.titleMedium)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        ResultRow("Metre (m)", calculatedPerimeter["m"] ?: "0.00")
                                        ResultRow("Kilometre (km)", calculatedPerimeter["km"] ?: "0.00")
                                        ResultRow("Mil (mi)", calculatedPerimeter["mi"] ?: "0.00")
                                        ResultRow("Fit (ft)", calculatedPerimeter["ft"] ?: "0.00")
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
                            onClick = {
                                pointList = ""
                                selectedPoints = emptyList()
                                calculatedArea = emptyMap()
                                calculatedPerimeter = emptyMap()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, "Temizle")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Temizle")
                        }

                        Button(
                            onClick = {
                                // Hesaplama simülasyonu
                                if (selectedTab == 0) {
                                    calculatedArea = mapOf(
                                        "m2" to "2,345.67",
                                        "km2" to "0.002346",
                                        "ha" to "0.234567",
                                        "donum" to "2.34567",
                                        "acre" to "0.57953"
                                    )
                                } else {
                                    calculatedPerimeter = mapOf(
                                        "m" to "234.56",
                                        "km" to "0.23456",
                                        "mi" to "0.14576",
                                        "ft" to "769.88"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Calculate, "Hesapla")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hesapla")
                        }
                    }
                }

                if (calculatedArea.isNotEmpty() || calculatedPerimeter.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* Paylaş */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, "Paylaş")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paylaş")
                            }

                            Button(
                                onClick = { /* Rapor oluştur */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, "PDF")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Rapor")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
