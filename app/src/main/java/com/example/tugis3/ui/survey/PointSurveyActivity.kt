@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@AndroidEntryPoint
class PointSurveyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                PointSurveyScreen(onBackPressed = { finish() })
            }
        }
    }
}

@Composable
fun PointSurveyScreen(onBackPressed: () -> Unit, vm: PointSurveyViewModel = hiltViewModel()) {
    val activeProject by vm.activeProject.collectAsState()
    val recentPoints by vm.recentPoints.collectAsState()

    // GPS durumu değişkenleri (simülasyon)
    var fixType by remember { mutableStateOf("No Fix") }
    var satelliteCount by remember { mutableStateOf(0) }
    var pdop by remember { mutableStateOf(99.9) }
    var hrms by remember { mutableStateOf(99.999) }
    var vrms by remember { mutableStateOf(99.999) }
    var age by remember { mutableStateOf(0) }

    // Konum bilgileri
    var latitude by remember { mutableStateOf(39.123456789) }
    var longitude by remember { mutableStateOf(32.654321012) }
    var elevation by remember { mutableStateOf(851.234) }
    var northing by remember { mutableStateOf(4330123.456) }
    var easting by remember { mutableStateOf(355678.912) }
    var zone by remember { mutableStateOf("35N") }

    // Nokta ölçüm ayarları
    var pointName by remember { mutableStateOf("P001") }
    var pointCode by remember { mutableStateOf("") }
    var antennaHeight by remember { mutableStateOf(2.000) }
    var measureTime by remember { mutableStateOf(5) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0) }

    val canRecord = fixType in listOf("RTK Float", "RTK Fixed") && activeProject != null

    // GPS simülasyonu
    LaunchedEffect(Unit) {
        while (true) {
            when (Random.nextInt(0, 100)) {
                in 0..5 -> {
                    fixType = "No Fix"; satelliteCount = Random.nextInt(0, 4); hrms = 99.999; vrms = 99.999
                }
                in 6..15 -> {
                    fixType = "2D Fix"; satelliteCount = Random.nextInt(4, 8); hrms = Random.nextDouble(5.0, 20.0); vrms = 99.999
                }
                in 16..40 -> {
                    fixType = "3D Fix"; satelliteCount = Random.nextInt(6, 12); hrms = Random.nextDouble(2.0, 8.0); vrms = Random.nextDouble(3.0, 12.0)
                }
                in 41..70 -> {
                    fixType = "RTK Float"; satelliteCount = Random.nextInt(8, 16); hrms = Random.nextDouble(0.5, 2.0); vrms = Random.nextDouble(0.8, 3.0)
                }
                else -> {
                    fixType = "RTK Fixed"; satelliteCount = Random.nextInt(10, 20); hrms = Random.nextDouble(0.005, 0.050); vrms = Random.nextDouble(0.008, 0.080)
                }
            }
            pdop = Random.nextDouble(1.0, 6.0)
            age = Random.nextInt(0, 30)
            if (fixType in listOf("RTK Float", "RTK Fixed")) {
                val variance = if (fixType == "RTK Fixed") 0.001 else 0.1
                latitude += Random.nextDouble(-variance, variance) / 100000
                longitude += Random.nextDouble(-variance, variance) / 100000
                elevation += Random.nextDouble(-variance * 10, variance * 10)
                northing = 4330000.0 + (latitude - 39.0) * 111000
                easting = 355000.0 + (longitude - 32.0) * 111000 * cos(latitude * PI / 180)
            }
            delay(1000)
        }
    }

    // Zamanlı kayıt
    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedTime = 0
            repeat(measureTime) {
                delay(1000); elapsedTime++
            }
            if (canRecord) {
                vm.savePoint(
                    name = pointName,
                    code = pointCode.ifBlank { null },
                    latitude = latitude,
                    longitude = longitude,
                    elevation = elevation,
                    northing = northing,
                    easting = easting,
                    zone = zone,
                    hrms = hrms,
                    vrms = vrms,
                    pdop = pdop,
                    satellites = satelliteCount,
                    fixType = fixType,
                    antennaHeight = antennaHeight
                )
                val number = pointName.filter { it.isDigit() }.toIntOrNull() ?: 1
                pointName = pointName.replace(number.toString(), (number + 1).toString().padStart(3, '0'))
            }
            isRecording = false; elapsedTime = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nokta Ölçümü") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
                actions = {
                    IconButton(onClick = {
                        val csv = vm.exportRecentPointsToCsv()
                        // TODO: Paylaş / dosyaya yazma entegrasyonu (şimdilik no-op)
                    }) { Icon(Icons.Default.FileDownload, contentDescription = "CSV Export") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (activeProject == null) {
                WarningCard("Aktif proje yok. Lütfen bir proje seçin/oluşturun.")
            }
            // GPS Durum Bilgisi
            StatusCard(fixType, satelliteCount, pdop, hrms, vrms)
            CoordinateCards(latitude, longitude, elevation, northing, easting, zone)
            PointInputCard(pointName, { pointName = it }, pointCode, { pointCode = it }, antennaHeight, {
                it.toDoubleOrNull()?.let { v -> if (v >= 0) antennaHeight = v }
            }, measureTime, {
                it.toIntOrNull()?.let { v -> if (v in 1..60) measureTime = v }
            }, isRecording)
            RecordControlCard(
                isRecording = isRecording,
                elapsedTime = elapsedTime,
                measureTime = measureTime,
                canRecord = canRecord,
                fixType = fixType,
                onStartTimed = { if (canRecord) isRecording = true },
                onQuick = {
                    if (canRecord) {
                        vm.savePoint(
                            name = pointName,
                            code = pointCode.ifBlank { null },
                            latitude = latitude,
                            longitude = longitude,
                            elevation = elevation,
                            northing = northing,
                            easting = easting,
                            zone = zone,
                            hrms = hrms,
                            vrms = vrms,
                            pdop = pdop,
                            satellites = satelliteCount,
                            fixType = fixType,
                            antennaHeight = antennaHeight
                        )
                        val number = pointName.filter { it.isDigit() }.toIntOrNull() ?: 1
                        pointName = pointName.replace(number.toString(), (number + 1).toString().padStart(3, '0'))
                    }
                },
                onCancel = { isRecording = false }
            )
            SatelliteStatusCard(satelliteCount, pdop, age)
            if (recentPoints.isNotEmpty()) {
                RecentPointsList(recentPoints)
            }
            BottomQuickRow()
        }
    }
}

@Composable private fun WarningCard(msg: String) { Card(modifier = Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0E0))) { Text(msg, modifier = Modifier.padding(12.dp), color = Color(0xFFB00020)) } }

@Composable private fun StatusCard(fixType: String, satelliteCount: Int, pdop: Double, hrms: Double, vrms: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (fixType) {
                "RTK Fixed" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                "RTK Float" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                "3D Fix" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                else -> Color(0xFFF44336).copy(alpha = 0.2f)
            }
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(fixType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = when (fixType) { "RTK Fixed" -> Color(0xFF4CAF50); "RTK Float" -> Color(0xFFFF9800); "3D Fix" -> Color(0xFF2196F3); else -> Color(0xFFF44336) })
                Text("Uydu: $satelliteCount | PDOP: ${String.format("%.1f", pdop)}", style = MaterialTheme.typography.bodySmall) }
            Column(horizontalAlignment = Alignment.End) { Text("H: ${String.format("%.3f", hrms)}m", style = MaterialTheme.typography.bodySmall); Text("V: ${String.format("%.3f", vrms)}m", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable private fun CoordinateCards(latitude: Double, longitude: Double, elevation: Double, northing: Double, easting: Double, zone: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Card(modifier = Modifier.weight(1f).padding(end = 4.dp)) { Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Coğrafi Koordinatlar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Enlem:", style = MaterialTheme.typography.bodySmall); Text(String.format("%.9f°", latitude), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("Boylam:", style = MaterialTheme.typography.bodySmall); Text(String.format("%.9f°", longitude), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("Yükseklik:", style = MaterialTheme.typography.bodySmall); Text("${String.format("%.3f", elevation)}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        } }
        Card(modifier = Modifier.weight(1f).padding(start = 4.dp)) { Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("UTM Koordinatlar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Kuzey (N):", style = MaterialTheme.typography.bodySmall); Text(String.format("%.3f", northing), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("Doğu (E):", style = MaterialTheme.typography.bodySmall); Text(String.format("%.3f", easting), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("Zone:", style = MaterialTheme.typography.bodySmall); Text(zone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        } }
    }
}

@Composable private fun PointInputCard(pointName: String, onName: (String)->Unit, pointCode: String, onCode: (String)->Unit, antenna: Double, onAntenna: (String)->Unit, measureTime: Int, onMeasure: (String)->Unit, isRecording: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) { Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Nokta Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = pointName, onValueChange = onName, label = { Text("Nokta Adı") }, modifier = Modifier.weight(1f), enabled = !isRecording, leadingIcon = { Icon(Icons.Default.LocationOn, null) })
            OutlinedTextField(value = pointCode, onValueChange = onCode, label = { Text("Kod") }, modifier = Modifier.weight(1f), enabled = !isRecording)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Anten Yüksekliği:", modifier = Modifier.weight(1f))
            OutlinedTextField(value = antenna.toString(), onValueChange = onAntenna, modifier = Modifier.width(120.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), suffix = { Text("m") }, enabled = !isRecording)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ölçüm Süresi:", modifier = Modifier.weight(1f))
            OutlinedTextField(value = measureTime.toString(), onValueChange = onMeasure, modifier = Modifier.width(120.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = { Text("s") }, enabled = !isRecording)
        }
    } }
}

@Composable private fun RecordControlCard(isRecording: Boolean, elapsedTime: Int, measureTime: Int, canRecord: Boolean, fixType: String, onStartTimed: ()->Unit, onQuick: ()->Unit, onCancel: ()->Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = if (isRecording) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isRecording) {
                Text("Nokta Kaydediliyor...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = elapsedTime.toFloat()/ measureTime.toFloat(), modifier = Modifier.fillMaxWidth())
                Text("${elapsedTime}/${measureTime} saniye")
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp)); Text("Durdur") }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onQuick, modifier = Modifier.weight(1f), enabled = canRecord && fixType in listOf("3D Fix","RTK Float","RTK Fixed")) { Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Hızlı") }
                    Button(onClick = onStartTimed, modifier = Modifier.weight(1f), enabled = canRecord) { Icon(Icons.Default.FiberManualRecord, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Kaydet") }
                }
                if (!canRecord) {
                    Text(if (fixType !in listOf("RTK Float","RTK Fixed")) "RTK Fix bekleniyor..." else "Aktif proje yok.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable private fun SatelliteStatusCard(satelliteCount: Int, pdop: Double, age: Int) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) { Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Uydu Durumu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) { drawSatelliteView(satelliteCount, pdop, size) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Uydu Sayısı: $satelliteCount"); Text("PDOP: ${String.format("%.1f", pdop)}"); Text("Age: ${age}s") }
    } }
}

@Composable private fun RecentPointsList(points: List<SurveyPointEntity>) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) { Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Kaydedilen Noktalar (${points.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(points) { point ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(point.name + (point.code?.let { " ($it)" } ?: ""), fontWeight = FontWeight.Bold)
                            Text(point.fixType ?: "", color = when (point.fixType) { "RTK Fixed" -> Color(0xFF4CAF50); "RTK Float" -> Color(0xFFFF9800); else -> Color(0xFF2196F3) }, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("N: ${point.northing?.let { String.format("%.3f", it) } ?: "-"} E: ${point.easting?.let { String.format("%.3f", it) } ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        Text("Z: ${point.elevation?.let { String.format("%.3f", it) } ?: "-"}m | H±${point.hrms?.let { String.format("%.3f", it) } ?: "-"}m V±${point.vrms?.let { String.format("%.3f", it) } ?: "-"}m", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    } }
}

@Composable private fun BottomQuickRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) { Text("Proje", style = MaterialTheme.typography.bodySmall) }
        OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) { Text("Cihaz", style = MaterialTheme.typography.bodySmall) }
        OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) { Text("Araçlar", style = MaterialTheme.typography.bodySmall) }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSatelliteView(
    satelliteCount: Int,
    pdop: Double,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
    val radius = minOf(canvasSize.width, canvasSize.height) / 3
    drawCircle(color = Color.Gray.copy(alpha = 0.2f), radius = radius, center = center)
    for (i in 0 until satelliteCount) {
        val angle = (i * 360.0 / (satelliteCount.coerceAtLeast(1))) * PI / 180
        val distance = radius * Random.nextDouble(0.3, 0.9)
        val satPos = Offset((center.x + distance * cos(angle)).toFloat(), (center.y + distance * sin(angle)).toFloat())
        drawCircle(color = when { pdop < 2.0 -> Color.Green; pdop < 4.0 -> Color.Yellow; else -> Color.Red }, radius = 4.dp.toPx(), center = satPos)
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = satPos)
    }
    drawCircle(color = Color.Blue, radius = 3.dp.toPx(), center = center)
}
