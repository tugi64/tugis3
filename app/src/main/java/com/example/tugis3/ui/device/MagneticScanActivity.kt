package com.example.tugis3.ui.device

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@AndroidEntryPoint
class MagneticScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                MagneticScanScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagneticScanScreen(onBackPressed: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    var magneticStrength by remember { mutableStateOf(45.2) }
    var magneticDeclination by remember { mutableStateOf(5.3) }
    var magneticInclination by remember { mutableStateOf(62.1) }
    var compassHeading by remember { mutableStateOf(180.0) }
    var calibrationQuality by remember { mutableStateOf("İyi") }
    var scanResults by remember { mutableStateOf<List<MagneticReading>>(emptyList()) }

    // Automatic magnetic field simulation
    LaunchedEffect(isScanning) {
        while (isScanning) {
            delay(500)
            magneticStrength = 45.0 + Random.nextDouble(-5.0, 5.0)
            compassHeading = (compassHeading + Random.nextDouble(-2.0, 2.0) + 360) % 360

            val reading = MagneticReading(
                timestamp = System.currentTimeMillis(),
                strength = magneticStrength,
                heading = compassHeading,
                x = magneticStrength * cos(Math.toRadians(compassHeading)) * cos(Math.toRadians(magneticInclination)),
                y = magneticStrength * sin(Math.toRadians(compassHeading)) * cos(Math.toRadians(magneticInclination)),
                z = magneticStrength * sin(Math.toRadians(magneticInclination))
            )

            scanResults = (scanResults + reading).takeLast(100)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manyetik Tarama") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Yenile */ }) { Icon(Icons.Default.Refresh, null) }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Manyetik Alan Durumu
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (calibrationQuality) {
                            "Mükemmel" -> MaterialTheme.colorScheme.primaryContainer
                            "İyi" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Manyetik Alan Durumu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Kalibrasyon: $calibrationQuality")
                                Text(String.format(Locale.US, "Alan Şiddeti: %.1f μT", magneticStrength))
                            }

                            Icon(
                                Icons.Default.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Alan Şiddeti:")
                            Text(String.format(Locale.US, "%.1f μT", magneticStrength), fontWeight = FontWeight.Medium)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Manyetik Sapma:")
                            Text(String.format(Locale.US, "%.1f°", magneticDeclination), fontWeight = FontWeight.Medium)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Manyetik Eğim:")
                            Text(String.format(Locale.US, "%.1f°", magneticInclination), fontWeight = FontWeight.Medium)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kompas Başlığı:")
                            Text(String.format(Locale.US, "%.1f°", compassHeading), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                // Kompas Görselleştirme
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Kompas Görünümü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val radius = minOf(size.width, size.height) / 3

                            // Kompas çemberi
                            drawCircle(
                                color = Color.Gray.copy(alpha = 0.3f),
                                radius = radius,
                                center = Offset(centerX, centerY)
                            )

                            // Yön işaretleri (angles only to avoid unused label)
                            val dirAngles = listOf(0f, 90f, 180f, 270f)
                            dirAngles.forEach { angle ->
                                val radian = Math.toRadians(angle.toDouble())
                                val x = centerX + (radius * 0.9f * sin(radian)).toFloat()
                                val y = centerY - (radius * 0.9f * cos(radian)).toFloat()

                                drawCircle(
                                    color = Color.Blue,
                                    radius = 8f,
                                    center = Offset(x, y)
                                )
                            }

                            // Manyetik kuzey
                            val magneticRadian = Math.toRadians(magneticDeclination)
                            val magX = centerX + (radius * 0.7f * sin(magneticRadian)).toFloat()
                            val magY = centerY - (radius * 0.7f * cos(magneticRadian)).toFloat()

                            drawLine(
                                color = Color.Red,
                                start = Offset(centerX, centerY),
                                end = Offset(magX, magY),
                                strokeWidth = 4f
                            )

                            // Kompas ibresi (mevcut yön)
                            val headingRadian = Math.toRadians(compassHeading)
                            val needleX = centerX + (radius * 0.8f * sin(headingRadian)).toFloat()
                            val needleY = centerY - (radius * 0.8f * cos(headingRadian)).toFloat()

                            drawLine(
                                color = Color.Green,
                                start = Offset(centerX, centerY),
                                end = Offset(needleX, needleY),
                                strokeWidth = 6f
                            )

                            // Merkez noktası
                            drawCircle(
                                color = Color.Black,
                                radius = 4f,
                                center = Offset(centerX, centerY)
                            )
                        }

                        Text(
                            "🔴 Manyetik Kuzey  🟢 Kompas İbresi  🔵 Coğrafi Yönler",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                // 3D Manyetik Vektör
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3D Manyetik Vektör", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        val currentReading = scanResults.lastOrNull()
                        if (currentReading != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("X Bileşeni:")
                                Text(String.format(Locale.US, "%.2f μT", currentReading.x), fontWeight = FontWeight.Medium)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Y Bileşeni:")
                                Text(String.format(Locale.US, "%.2f μT", currentReading.y), fontWeight = FontWeight.Medium)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Z Bileşeni:")
                                Text(String.format(Locale.US, "%.2f μT", currentReading.z), fontWeight = FontWeight.Medium)
                            }

                            Spacer(Modifier.height(8.dp))

                            val magnitude = sqrt(currentReading.x.pow(2) + currentReading.y.pow(2) + currentReading.z.pow(2))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Toplam Büyüklük:")
                                Text(String.format(Locale.US, "%.2f μT", magnitude), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Veri bekleniyor...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                // Tarama Kontrolü
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    if (isScanning) "Tarama Aktif" else "Tarama Durduruldu",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${scanResults.size} okuma yapıldı")
                            }

                            if (isScanning) {
                                Button(
                                    onClick = { isScanning = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Stop, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Durdur")
                                }
                            } else {
                                Button(
                                    onClick = { isScanning = true }
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Başlat")
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Kalibrasyon ve Ayarlar
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Kalibrasyon ve Ayarlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    calibrationQuality = "Mükemmel"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Tune, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Kalibrasyon")
                            }

                            OutlinedButton(
                                onClick = {
                                    scanResults = emptyList()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Clear, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Temizle")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Kalibrasyon için cihazı 8 şeklinde hareket ettirin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (scanResults.isNotEmpty()) {
                item {
                    // Tarama Sonuçları
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Son Tarama Sonuçları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Spacer(Modifier.height(8.dp))

                            val lastReadings = scanResults.takeLast(5)
                            lastReadings.forEach { reading ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(String.format(Locale.US, "%.1f μT", reading.strength), fontWeight = FontWeight.Medium)
                                        Text(String.format(Locale.US, "%.1f°", reading.heading))
                                        Text(java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(reading.timestamp)))
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { /* Export data */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileDownload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Verileri Dışa Aktar")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MagneticReading(
    val timestamp: Long,
    val strength: Double,
    val heading: Double,
    val x: Double,
    val y: Double,
    val z: Double
)
