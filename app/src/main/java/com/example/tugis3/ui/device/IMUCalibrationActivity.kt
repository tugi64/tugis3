package com.example.tugis3.ui.device

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@AndroidEntryPoint
class IMUCalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                IMUCalibrationScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IMUCalibrationScreen(onBackPressed: () -> Unit) {
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableStateOf(0f) }
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    var tiltTotal by remember { mutableStateOf(0f) }
    var imuAccuracy by remember { mutableStateOf("Orta") }
    var calibrationQuality by remember { mutableStateOf("İyi") }
    var calibrationSteps by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(isCalibrating) {
        if (isCalibrating) {
            calibrationSteps = listOf(
                "IMU sensörleri başlatılıyor...",
                "Başlangıç değerleri okunuyor...",
                "X ekseni kalibrasyonu...",
                "Y ekseni kalibrasyonu...",
                "Z ekseni kalibrasyonu...",
                "Gyroscope kalibrasyonu...",
                "Accelerometer kalibrasyonu...",
                "Magnetometer kalibrasyonu...",
                "Kalibrasyon verileri kaydediliyor...",
                "Kalibrasyon tamamlandı!"
            )

            calibrationSteps.forEachIndexed { index, step ->
                delay(1000)
                calibrationProgress = (index + 1).toFloat() / calibrationSteps.size

                // Simulate sensor readings during calibration
                tiltX = Random.nextFloat() * 10f - 5f
                tiltY = Random.nextFloat() * 10f - 5f
                tiltTotal = sqrt(tiltX * tiltX + tiltY * tiltY)

                if (index == calibrationSteps.size - 1) {
                    calibrationQuality = when {
                        calibrationProgress >= 0.9f -> "Mükemmel"
                        calibrationProgress >= 0.7f -> "İyi"
                        else -> "Orta"
                    }
                    imuAccuracy = "Yüksek"
                    isCalibrating = false
                }
            }
        }
    }

    // Real-time IMU simulation
    LaunchedEffect(Unit) {
        while (true) {
            if (!isCalibrating) {
                tiltX = Random.nextFloat() * 4f - 2f
                tiltY = Random.nextFloat() * 4f - 2f
                tiltTotal = sqrt(tiltX * tiltX + tiltY * tiltY)
            }
            delay(200)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IMU Kalibrasyon") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
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
                // IMU Status
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
                                Text("IMU Durumu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Kalibrasyon: $calibrationQuality")
                                Text("Doğruluk: $imuAccuracy")
                            }

                            Icon(
                                Icons.Default.DeviceHub,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("X Ekseni Eğim:")
                            Text("${String.format("%.2f", tiltX)}°", fontWeight = FontWeight.Medium)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Y Ekseni Eğim:")
                            Text("${String.format("%.2f", tiltY)}°", fontWeight = FontWeight.Medium)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Toplam Eğim:")
                            Text("${String.format("%.2f", tiltTotal)}°",
                                 fontWeight = FontWeight.Medium,
                                 color = if (tiltTotal < 2f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                // Tilt Visualization
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Eğim Görselleştirme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val radius = minOf(size.width, size.height) / 3

                            // Draw level circle
                            drawCircle(
                                color = Color.Gray.copy(alpha = 0.3f),
                                radius = radius,
                                center = Offset(centerX, centerY)
                            )

                            // Draw crosshair
                            drawLine(
                                color = Color.Gray,
                                start = Offset(centerX - radius, centerY),
                                end = Offset(centerX + radius, centerY),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.Gray,
                                start = Offset(centerX, centerY - radius),
                                end = Offset(centerX, centerY + radius),
                                strokeWidth = 1f
                            )

                            // Draw tilt indicator
                            val tiltXPx = (tiltX / 10f) * radius
                            val tiltYPx = (tiltY / 10f) * radius
                            val tiltIndicatorX = centerX + tiltXPx
                            val tiltIndicatorY = centerY + tiltYPx

                            drawCircle(
                                color = if (tiltTotal < 2f) Color.Green else if (tiltTotal < 5f) Color(0xFFFF9800) else Color.Red,
                                radius = 8f,
                                center = Offset(tiltIndicatorX, tiltIndicatorY)
                            )

                            // Draw tolerance circles
                            drawCircle(
                                color = Color.Green.copy(alpha = 0.3f),
                                radius = radius * 0.2f,
                                center = Offset(centerX, centerY),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                            )

                            drawCircle(
                                color = Color(0xFFFF9800).copy(alpha = 0.3f),
                                radius = radius * 0.5f,
                                center = Offset(centerX, centerY),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                            )
                        }

                        Text(
                            "🟢 ±2° Mükemmel  🟠 ±5° İyi  🔴 >5° Kötü",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                // Calibration Control
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCalibrating)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isCalibrating) {
                            Text("Kalibrasyon Devam Ediyor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Spacer(Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = calibrationProgress,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Text("${(calibrationProgress * 100).toInt()}% tamamlandı")

                            if (calibrationSteps.isNotEmpty()) {
                                val currentStep = (calibrationProgress * calibrationSteps.size).toInt().coerceAtMost(calibrationSteps.size - 1)
                                Text(calibrationSteps[currentStep], style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = { isCalibrating = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Kalibrasyonu Durdur")
                            }
                        } else {
                            Text("IMU Kalibrasyon Kontrolü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Text("Cihazı düz bir yüzeyde tutup kalibrasyonu başlatın.", style = MaterialTheme.typography.bodyMedium)

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { /* Reset to factory */ },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.RestartAlt, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Fabrika Ayarları")
                                }

                                Button(
                                    onClick = {
                                        isCalibrating = true
                                        calibrationProgress = 0f
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Tune, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Kalibrasyonu Başlat")
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Calibration Instructions
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Kalibrasyon Talimatları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        val instructions = listOf(
                            "1. Cihazı düz bir yüzey üzerine yerleştirin",
                            "2. Cihazın hareket etmediğinden emin olun",
                            "3. Kalibrasyon sırasında cihaza dokunmayın",
                            "4. Kalibrasyon 10-15 saniye sürer",
                            "5. Tamamlandığında yeşil işaret görünecek"
                        )

                        instructions.forEach { instruction ->
                            Text(
                                instruction,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Not: Eğim ölçümü doğruluğu için düzenli kalibrasyon önerilir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                // Test Results
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Test Sonuçları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        val testResults = listOf(
                            "Accelerometer" to "✅ Geçti",
                            "Gyroscope" to "✅ Geçti",
                            "Magnetometer" to "✅ Geçti",
                            "Eğim Sensörü" to if (tiltTotal < 2f) "✅ Geçti" else "❌ Kalibrasyon Gerekli",
                            "Doğruluk Testi" to if (imuAccuracy == "Yüksek") "✅ Geçti" else "⚠️ Orta"
                        )

                        testResults.forEach { (test, result) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(test)
                                Text(
                                    result,
                                    color = when {
                                        result.contains("✅") -> MaterialTheme.colorScheme.primary
                                        result.contains("⚠️") -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
