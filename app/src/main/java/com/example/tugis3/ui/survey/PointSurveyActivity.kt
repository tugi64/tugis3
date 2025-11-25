package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

@AndroidEntryPoint
class PointSurveyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                PointSurveyMainScreen(onBack = { finish() })
            }
        }
    }
}

// Veri Modelleri
data class SurveyPoint(
    val name: String = "P001",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val type: String = "Detay",
    val description: String = "",
    val code: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointSurveyMainScreen(onBack: () -> Unit) {
    var currentPoint by remember { mutableStateOf(SurveyPoint()) }
    var points by remember { mutableStateOf(listOf<SurveyPoint>()) }
    var isCollecting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var showSettings by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // GnssPositionRepository'den gerçek GPS verilerini al
    val gnssPosition by com.example.tugis3.gnss.GnssPositionRepository.position.collectAsState()

    // GPS verilerini currentPoint'e aktar
    LaunchedEffect(gnssPosition) {
        if (gnssPosition.lat != null && gnssPosition.lon != null) {
            // WGS84 (lat/lon) koordinatlarını kullan
            // Gerçek uygulamada koordinat dönüşümü yapılmalı (WGS84 -> UTM/TM veya proje koordinat sistemi)
            currentPoint = currentPoint.copy(
                x = gnssPosition.lon ?: currentPoint.x,  // Longitude -> X (Doğu)
                y = gnssPosition.lat ?: currentPoint.y,  // Latitude -> Y (Kuzey)
                z = gnssPosition.alt ?: currentPoint.z   // Altitude -> Z (Yükseklik)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nokta Aplikasyonu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    BadgedBox(
                        badge = { if (points.isNotEmpty()) Badge { Text("${points.size}") } }
                    ) {
                        IconButton(onClick = { showList = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, "Liste")
                        }
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Ayarlar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (fabExpanded) {
                    SmallFloatingActionButton(
                        onClick = {
                            if (currentPoint.name.isNotBlank()) {
                                points = points + currentPoint
                                scope.launch {
                                    snackbar.showSnackbar("${currentPoint.name} kaydedildi")
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.Save, "Kaydet")
                    }
                }

                LargeFloatingActionButton(
                    onClick = {
                        if (!isCollecting && !fabExpanded) {
                            isCollecting = true
                            scope.launch {
                                for (i in 1..10) {
                                    delay(1000)
                                    progress = i / 10f
                                }
                                isCollecting = false
                            }
                        } else {
                            fabExpanded = !fabExpanded
                        }
                    },
                    containerColor = if (isCollecting) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                ) {
                    if (isCollecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Icon(
                            if (fabExpanded) Icons.Default.Close else Icons.Default.MyLocation,
                            if (fabExpanded) "Kapat" else "Ölç",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // GPS Status
            item {
                // GnssPositionRepository'den gerçek zamanlı verileri al
                val gnssPosition by com.example.tugis3.gnss.GnssPositionRepository.position.collectAsState()

                val fixType = gnssPosition.fixType ?: com.example.tugis3.gnss.model.FixType.NO_FIX
                val satInUse = gnssPosition.satsInUse ?: 0
                val satVisible = gnssPosition.satsVisible ?: 0
                val hdop = gnssPosition.hdop ?: 99.9

                // Fix durumuna göre renk ve ikon
                val (statusColor, statusIcon, statusText) = when (fixType) {
                    com.example.tugis3.gnss.model.FixType.RTK_FIX -> Triple(Color(0xFF4CAF50), Icons.Default.GpsFixed, "RTK Fixed")
                    com.example.tugis3.gnss.model.FixType.RTK_FLOAT -> Triple(Color(0xFFFFA726), Icons.Default.GpsNotFixed, "RTK Float")
                    com.example.tugis3.gnss.model.FixType.DGPS -> Triple(Color(0xFF42A5F5), Icons.Default.GpsNotFixed, "DGPS")
                    com.example.tugis3.gnss.model.FixType.SINGLE -> Triple(Color(0xFFFFEB3B), Icons.Default.GpsNotFixed, "Tek Frekans")
                    else -> Triple(Color(0xFFF44336), Icons.Default.GpsOff, "GPS Yok")
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(statusIcon, null, tint = statusColor)
                            Column {
                                Text(statusText, fontWeight = FontWeight.Bold)
                                val satText = if (satVisible > 0) "$satInUse/$satVisible uydu" else if (satInUse > 0) "$satInUse uydu" else "0 uydu"
                                Text(satText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            // HDOP'a göre hassasiyet tahmini (RTK'da cm, DGPS'te dm, Single'da m)
                            val accuracyText = when (fixType) {
                                com.example.tugis3.gnss.model.FixType.RTK_FIX -> String.format(Locale.US, "%.1f cm", hdop * 1.0)
                                com.example.tugis3.gnss.model.FixType.RTK_FLOAT -> String.format(Locale.US, "%.0f cm", hdop * 10.0)
                                com.example.tugis3.gnss.model.FixType.DGPS -> String.format(Locale.US, "%.1f dm", hdop * 3.0)
                                com.example.tugis3.gnss.model.FixType.SINGLE -> String.format(Locale.US, "%.1f m", hdop * 5.0)
                                else -> "---"
                            }
                            Text(accuracyText, fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.US, "HDOP: %.1f", hdop), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Nokta Bilgileri
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Nokta Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = currentPoint.name,
                            onValueChange = { currentPoint = currentPoint.copy(name = it) },
                            label = { Text("Nokta Adı") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Pin, null) }
                        )

                        OutlinedTextField(
                            value = currentPoint.code,
                            onValueChange = { currentPoint = currentPoint.copy(code = it) },
                            label = { Text("Kod") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.QrCode, null) }
                        )

                        OutlinedTextField(
                            value = currentPoint.description,
                            onValueChange = { currentPoint = currentPoint.copy(description = it) },
                            label = { Text("Açıklama") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }
            }

            // Koordinatlar
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Koordinatlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("X (Doğu)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "%.3f", currentPoint.x),
                                fontWeight = FontWeight.Medium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Y (Kuzey)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "%.3f", currentPoint.y),
                                fontWeight = FontWeight.Medium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Z (Yükseklik)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "%.3f", currentPoint.z),
                                fontWeight = FontWeight.Medium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Progress
            if (isCollecting) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Veri Toplanıyor", fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${(progress * 100).toInt()}% - ${(progress * 10).toInt()}/10 epoch")
                        }
                    }
                }
            }

            // Son Noktalar
            if (points.isNotEmpty()) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Son Ölçümler (${points.size})", fontWeight = FontWeight.Bold)

                            points.takeLast(3).forEach { point ->
                                ElevatedCard(
                                    onClick = { currentPoint = point },
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(point.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${point.type}${if (point.code.isNotEmpty()) " • ${point.code}" else ""}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showList) {
        AlertDialog(
            onDismissRequest = { showList = false },
            title = { Text("Nokta Listesi (${points.size})") },
            text = {
                LazyColumn {
                    items(points.reversed()) { point ->
                        ListItem(
                            headlineContent = { Text(point.name) },
                            supportingContent = { Text("X: ${String.format(Locale.US, "%.3f", point.x)}") },
                            trailingContent = {
                                IconButton(onClick = { points = points - point }) {
                                    Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showList = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Ayarlar") },
            text = { Text("Ayarlar menüsü burada geliştirilecek") },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Tamam")
                }
            }
        )
    }
}

