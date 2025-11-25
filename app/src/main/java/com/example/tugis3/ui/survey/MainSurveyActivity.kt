package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tugis3.ui.theme.Tugis3Theme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.tugis3.ui.device.GnssDeviceViewModel
import com.example.tugis3.ui.map.GraphicSurveyScreen
import com.example.tugis3.ui.survey.model.SurveyPoint

// Veri modelleri
data class GnssStatus(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val hrms: Double = 0.0,
    val vrms: Double = 0.0,
    val pdop: Double = 0.0,
    val hdop: Double = 0.0,
    val vdop: Double = 0.0,
    val satelliteCount: Int = 0,
    val batteryLevel: Int = 100,
    val isFixed: Boolean = false,
    val sleepStatus: String = "UYKU DURUMU"
)

enum class SurveyMode {
    STANDART, EPOKLU, HIZLI, OTOMATIK
}

class MainSurveyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                SurveyMainScreen()
            }
        }
    }
}

@Composable
fun SurveyMainScreen(
    viewModel: GnssDeviceViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Nokta Uygulaması", "Grafik Alım")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }

        when (selectedTab) {
            0 -> MainPointSurveyScreen(viewModel = viewModel)
            1 -> GraphicSurveyScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun MainPointSurveyScreen(
    viewModel: GnssDeviceViewModel = hiltViewModel()
) {
    var selectedMode by remember { mutableStateOf(SurveyMode.STANDART) }
    var gnssStatus by remember { mutableStateOf(GnssStatus()) }
    var surveyPoints by remember { mutableStateOf(listOf<SurveyPoint>()) }
    var isRecording by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPointList by remember { mutableStateOf(false) }
    var currentPointName by remember { mutableStateOf("") }
    var pointDescription by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Status Bar
        TopStatusBar(gnssStatus = gnssStatus)

        // Survey Mode Selection
        SurveyModeSelector(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it }
        )

        // Main Content
        Row(modifier = Modifier.weight(1f)) {
            // Left Panel - Tools
            ToolsPanel(
                modifier = Modifier.width(80.dp),
                onSettingsClick = { showSettings = true },
                onPointListClick = { showPointList = true }
            )

            // Center Panel - Main Survey Area
            Column(
                modifier = Modifier.weight(1f).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // GNSS Details Card
                GnssDetailsCard(
                    gnssStatus = gnssStatus,
                    selectedMode = selectedMode
                )

                // Point Information Input
                PointInformationCard(
                    pointName = currentPointName,
                    onPointNameChange = { currentPointName = it },
                    description = pointDescription,
                    onDescriptionChange = { pointDescription = it }
                )

                // Recording Controls
                RecordingControlsCard(
                    isRecording = isRecording,
                    onStartRecording = {
                        isRecording = true
                        // Nokta kaydetme işlemi
                    },
                    onStopRecording = { isRecording = false },
                    selectedMode = selectedMode
                )
            }
        }

        // Bottom Status
        BottomStatusBar(
            pointCount = surveyPoints.size,
            isRecording = isRecording
        )
    }

    // Dialogs
    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }

    if (showPointList) {
        PointListDialog(
            points = surveyPoints,
            onDismiss = { showPointList = false }
        )
    }
}

@Composable
fun TopStatusBar(gnssStatus: GnssStatus) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Çözüm Durumu", fontWeight = FontWeight.Bold)
            Text("Hatalar", fontWeight = FontWeight.Bold)
            Text("Uydu Durumu", fontWeight = FontWeight.Bold)

            // GNSS Receiver Battery Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Battery3Bar, contentDescription = "Battery")
                Text("${gnssStatus.batteryLevel}%")
            }
        }
    }
}

@Composable
fun SurveyModeSelector(
    selectedMode: SurveyMode,
    onModeSelected: (SurveyMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Alım Modu", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SurveyMode.values().forEach { mode ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onModeSelected(mode) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMode == mode)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                when (mode) {
                                    SurveyMode.STANDART -> Icons.Default.RadioButtonChecked
                                    SurveyMode.EPOKLU -> Icons.Default.Timeline
                                    SurveyMode.HIZLI -> Icons.Default.Speed
                                    SurveyMode.OTOMATIK -> Icons.Default.AutoMode
                                },
                                contentDescription = mode.name,
                                tint = if (selectedMode == mode) Color.White else Color.Black
                            )
                            Text(
                                mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 10.sp,
                                color = if (selectedMode == mode) Color.White else Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsPanel(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onPointListClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tool buttons
            ToolButton(Icons.Default.Settings, "Ayarlar", onSettingsClick)
            ToolButton(Icons.Default.List, "Nokta Listesi", onPointListClick)
            ToolButton(Icons.Default.Save, "Kaydet") { }
            ToolButton(Icons.Default.FileOpen, "Aç") { }
            ToolButton(Icons.Default.Share, "Paylaş") { }

            Spacer(modifier = Modifier.weight(1f))

            // Survey button
            Card(
                modifier = Modifier.size(60.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Alım Butonu",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.size(50.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun GnssDetailsCard(
    gnssStatus: GnssStatus,
    selectedMode: SurveyMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = when (selectedMode) {
                    SurveyMode.STANDART -> "Standart Alım Modu"
                    SurveyMode.EPOKLU -> "Epoklu Alım Modu"
                    SurveyMode.HIZLI -> "Hızlı Alım Modu"
                    SurveyMode.OTOMATIK -> "Otomatik Alım Modu"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // GNSS Koordinat ve Doğruluk Bilgileri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow("Kod", "P1")
                    InfoRow("Kuzey", String.format("%.6f", gnssStatus.latitude))
                    InfoRow("Doğu", String.format("%.6f", gnssStatus.longitude))
                    InfoRow("Y", String.format("%.3f", gnssStatus.altitude))
                    InfoRow("X", String.format("%.3f", gnssStatus.longitude))
                    InfoRow("Yükseklik (Z)", String.format("%.3f", gnssStatus.altitude))
                }

                Column(modifier = Modifier.weight(1f)) {
                    InfoRow("Anten Y", String.format("%.3f", gnssStatus.altitude))
                    InfoRow("HRMS", String.format("%.3f", gnssStatus.hrms))
                    InfoRow("VRMS", String.format("%.3f", gnssStatus.vrms))
                    InfoRow("Gecikme", "1")
                    InfoRow("PDOP", String.format("%.1f", gnssStatus.pdop))
                    InfoRow("Uydu Sayısı", "${gnssStatus.satelliteCount}")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", fontSize = 12.sp)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PointInformationCard(
    pointName: String,
    onPointNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Nokta Bilgileri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pointName,
                onValueChange = onPointNameChange,
                label = { Text("Nokta Adı") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Açıklama") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
        }
    }
}

@Composable
fun RecordingControlsCard(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    selectedMode: SurveyMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Alım Kontrolleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedMode) {
                SurveyMode.STANDART -> {
                    Button(
                        onClick = if (isRecording) onStopRecording else onStartRecording,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isRecording) "Tamamlı" else "Fotoğraf ve Kroki")
                    }
                }

                SurveyMode.EPOKLU -> {
                    Column {
                        Text("Epok okuması: 10 epok, 4 saniye aralık")
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isRecording) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = 0.6f // Örnek ilerleme
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = if (isRecording) onStopRecording else onStartRecording,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isRecording) "Durdur" else "Başlat")
                        }
                    }
                }

                SurveyMode.HIZLI -> {
                    Button(
                        onClick = onStartRecording,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hızlı Alım")
                    }
                }

                SurveyMode.OTOMATIK -> {
                    Column {
                        Text("Otomatik alım modu aktif")
                        Text("Mesafe kriteri: 2m, Zaman kriteri: 5s")
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Başlat")
                            }
                            Button(
                                onClick = { },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Duraklat")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomStatusBar(
    pointCount: Int,
    isRecording: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nokta Sayısı: $pointCount")
            Text("Bilgi Çubuğu")
            Text("Ayarlar")

            // Status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (isRecording) Color.Red else Color.Green,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isRecording) "Kaydediyor" else "Hazır")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekran Bilgileri") },
        text = {
            Column {
                Text("HRMS Limiti: 0.05")
                Text("VRMS Limiti: 0.1")
                Text("PDOP Limiti: 3.0")
                Text("Gecikme Limiti: 5")
                Text("Aynı İşlem Kullanımı: Kapalı")
                Text("Girilen Son Noktaya Odaklan: Açık")
                Text("Nokta Adı: P1")
                Text("Nokta Adı Artışı: 1")
                Text("Varsayılan Kod: Son Nokta ile Aynı")
                Text("Ölçüm Süresi: 3")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("TAMAM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("VARSAYILAN")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointListDialog(
    points: List<SurveyPoint>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nokta Listesi") },
        text = {
            LazyColumn {
                items(points) { point ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(point.name, fontWeight = FontWeight.Bold)
                            Text("X: ${String.format("%.3f", point.x)}")
                            Text("Y: ${String.format("%.3f", point.y)}")
                            Text("Z: ${String.format("%.3f", point.z)}")
                            Text("Doğruluk: ${String.format("%.3f", point.accuracy)}m")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
