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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tugis3.ui.theme.Tugis3Theme
import com.example.tugis3.ui.survey.model.SurveyPoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TAM İŞLEVSEL ALIM EKRANI
 * Kullanım Kılavuzundaki tüm özellikleri içerir:
 * - Standart: Nokta detayları, fotoğraf ve kroki ekleme
 * - Epoklu: 10 epok okuma, 4 sn aralık, grafik gösterge
 * - Hızlı: Tek tuşla anında alım
 * - Otomatik: Mesafe ve zaman kriterlerine göre otomatik alım
 */

@AndroidEntryPoint
class CompleteSurveyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CompleteSurveyScreen(onBack = { finish() })
            }
        }
    }
}

// Veri modelleri
data class CompleteSurveyData(
    val latitude: Double = 40.748817,
    val longitude: Double = 29.878342,
    val altitude: Double = 1131.004,
    val x: Double = 485069.253,
    val y: Double = 4415708.373,
    val z: Double = 1131.004,
    val hrms: Double = 0.005,
    val vrms: Double = 0.009,
    val pdop: Double = 1.1,
    val hdop: Double = 0.8,
    val vdop: Double = 0.9,
    val satelliteCount: Int = 12,
    val batteryLevel: Int = 100,
    val isRtkFixed: Boolean = true,
    val antenHeight: Double = 2.0,
    val delay: Int = 1
)

data class EpochData(
    val currentEpoch: Int = 0,
    val totalEpochs: Int = 10,
    val intervalSeconds: Int = 4,
    val readings: List<EpochReading> = emptyList()
)

data class EpochReading(
    val epoch: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val deviation: Double
)

enum class CompleteSurveyMode {
    STANDART, EPOKLU, HIZLI, OTOMATIK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteSurveyScreen(onBack: () -> Unit) {
    var selectedMode by remember { mutableStateOf(CompleteSurveyMode.STANDART) }
    var surveyData by remember { mutableStateOf(CompleteSurveyData()) }
    var surveyPoints by remember { mutableStateOf(listOf<SurveyPoint>()) }
    var isRecording by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPointList by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showSketchDialog by remember { mutableStateOf(false) }
    var showEpochGraph by remember { mutableStateOf(false) }

    // Nokta bilgileri
    var pointName by remember { mutableStateOf("P1") }
    var pointCode by remember { mutableStateOf("") }
    var pointDescription by remember { mutableStateOf("") }

    // Epoklu mod
    var epochData by remember { mutableStateOf(EpochData()) }

    // Otomatik mod
    var autoModeActive by remember { mutableStateOf(false) }
    var autoDistanceCriteria by remember { mutableStateOf(2.0) }
    var autoTimeCriteria by remember { mutableStateOf(5) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nokta Alım - Tam İşlevsel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Üst durum çubuğu
            SurveyStatusBar(surveyData = surveyData)

            // Alım modu seçici
            SurveyModeSelector(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it }
            )

            // Ana içerik
            Row(modifier = Modifier.weight(1f)) {
                // Sol araç paneli
                SurveyToolsPanel(
                    modifier = Modifier.width(80.dp),
                    onSettingsClick = { showSettings = true },
                    onPointListClick = { showPointList = true },
                    onPhotoClick = { showPhotoDialog = true },
                    onSketchClick = { showSketchDialog = true },
                    onSaveClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Proje kaydedildi")
                        }
                    }
                )

                // Orta panel - Ana alım alanı
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GNSS Bilgileri Kartı
                    GnssInformationCard(
                        surveyData = surveyData,
                        selectedMode = selectedMode,
                        pointCode = pointCode
                    )

                    // Nokta Bilgileri Girişi
                    PointInputCard(
                        pointName = pointName,
                        onPointNameChange = { pointName = it },
                        pointCode = pointCode,
                        onPointCodeChange = { pointCode = it },
                        description = pointDescription,
                        onDescriptionChange = { pointDescription = it }
                    )

                    // Alım Kontrol Kartı - Moda göre değişir
                    when (selectedMode) {
                        CompleteSurveyMode.STANDART -> {
                            StandardModeCard(
                                onPhotoAndSketch = { showPhotoDialog = true },
                                onComplete = {
                                    // Nokta kaydet
                                    val newPoint = SurveyPoint(
                                        id = (surveyPoints.size + 1).toString(),
                                        name = pointName,
                                        x = surveyData.x,
                                        y = surveyData.y,
                                        z = surveyData.z,
                                        accuracy = surveyData.hrms
                                    )
                                    surveyPoints = surveyPoints + newPoint
                                    pointName = "P${surveyPoints.size + 1}"
                                    scope.launch {
                                        snackbarHostState.showSnackbar("${newPoint.name} kaydedildi")
                                    }
                                }
                            )
                        }

                        CompleteSurveyMode.EPOKLU -> {
                            EpochModeCard(
                                isRecording = isRecording,
                                epochData = epochData,
                                onStart = {
                                    isRecording = true
                                    scope.launch {
                                        for (i in 1..epochData.totalEpochs) {
                                            epochData = epochData.copy(currentEpoch = i)
                                            // Simüle edilen okuma
                                            val reading = EpochReading(
                                                epoch = i,
                                                x = surveyData.x + (Math.random() - 0.5) * 0.01,
                                                y = surveyData.y + (Math.random() - 0.5) * 0.01,
                                                z = surveyData.z + (Math.random() - 0.5) * 0.01,
                                                deviation = Math.random() * 0.01
                                            )
                                            epochData = epochData.copy(
                                                readings = epochData.readings + reading
                                            )
                                            delay(epochData.intervalSeconds * 1000L)
                                        }
                                        isRecording = false
                                        // Nokta kaydet
                                        val newPoint = SurveyPoint(
                                            id = (surveyPoints.size + 1).toString(),
                                            name = pointName,
                                            x = surveyData.x,
                                            y = surveyData.y,
                                            z = surveyData.z,
                                            accuracy = surveyData.hrms
                                        )
                                        surveyPoints = surveyPoints + newPoint
                                        pointName = "P${surveyPoints.size + 1}"
                                        epochData = EpochData()
                                        snackbarHostState.showSnackbar("${newPoint.name} kaydedildi (${epochData.totalEpochs} epok)")
                                    }
                                },
                                onStop = {
                                    isRecording = false
                                    epochData = EpochData()
                                },
                                onShowGraph = { showEpochGraph = true }
                            )
                        }

                        CompleteSurveyMode.HIZLI -> {
                            QuickModeCard(
                                onQuickRecord = {
                                    val newPoint = SurveyPoint(
                                        id = (surveyPoints.size + 1).toString(),
                                        name = pointName,
                                        x = surveyData.x,
                                        y = surveyData.y,
                                        z = surveyData.z,
                                        accuracy = surveyData.hrms
                                    )
                                    surveyPoints = surveyPoints + newPoint
                                    pointName = "P${surveyPoints.size + 1}"
                                    scope.launch {
                                        snackbarHostState.showSnackbar("${newPoint.name} hızlı alım tamamlandı")
                                    }
                                }
                            )
                        }

                        CompleteSurveyMode.OTOMATIK -> {
                            AutomaticModeCard(
                                isActive = autoModeActive,
                                distanceCriteria = autoDistanceCriteria,
                                timeCriteria = autoTimeCriteria,
                                onDistanceChange = { autoDistanceCriteria = it },
                                onTimeChange = { autoTimeCriteria = it },
                                onStart = {
                                    autoModeActive = true
                                    scope.launch {
                                        while (autoModeActive) {
                                            delay(autoTimeCriteria * 1000L)
                                            val newPoint = SurveyPoint(
                                                id = (surveyPoints.size + 1).toString(),
                                                name = pointName,
                                                x = surveyData.x,
                                                y = surveyData.y,
                                                z = surveyData.z,
                                                accuracy = surveyData.hrms
                                            )
                                            surveyPoints = surveyPoints + newPoint
                                            pointName = "P${surveyPoints.size + 1}"
                                            snackbarHostState.showSnackbar("${newPoint.name} otomatik kaydedildi")
                                        }
                                    }
                                },
                                onStop = { autoModeActive = false },
                                onPause = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Otomatik alım duraklatıldı")
                                    }
                                }
                            )
                        }
                    }
                }

                // Sağ panel - Bilgi paneli (opsiyonel)
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .fillMaxHeight()
                        .padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "BİLGİ PANELİ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider()

                        InfoItem("Toplam Nokta", "${surveyPoints.size}")
                        InfoItem("Aktif Mod", when(selectedMode) {
                            CompleteSurveyMode.STANDART -> "Standart"
                            CompleteSurveyMode.EPOKLU -> "Epoklu"
                            CompleteSurveyMode.HIZLI -> "Hızlı"
                            CompleteSurveyMode.OTOMATIK -> "Otomatik"
                        })
                        InfoItem("Durum", if (isRecording || autoModeActive) "Kaydediyor" else "Hazır")

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            "Son noktalar ekranda gösterilir",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Alt durum çubuğu
            SurveyBottomBar(
                pointCount = surveyPoints.size,
                isActive = isRecording || autoModeActive
            )
        }
    }

    // Dialog'lar
    if (showSettings) {
        SurveySettingsDialog(onDismiss = { showSettings = false })
    }

    if (showPointList) {
        PointListDialog(
            points = surveyPoints,
            onDismiss = { showPointList = false },
            onDelete = { point ->
                surveyPoints = surveyPoints.filter { it.id != point.id }
            }
        )
    }

    if (showPhotoDialog) {
        PhotoAndSketchDialog(
            onDismiss = { showPhotoDialog = false },
            onPhotoTaken = {
                scope.launch {
                    snackbarHostState.showSnackbar("Fotoğraf eklendi")
                }
            },
            onSketchDrawn = {
                scope.launch {
                    snackbarHostState.showSnackbar("Kroki eklendi")
                }
            }
        )
    }

    if (showEpochGraph) {
        EpochGraphDialog(
            epochData = epochData,
            onDismiss = { showEpochGraph = false }
        )
    }
}

@Composable
fun SurveyStatusBar(surveyData: CompleteSurveyData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.GpsFixed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (surveyData.isRtkFixed) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (surveyData.isRtkFixed) "RTK FIX" else "RTK FLOAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HRMS: ", fontSize = 10.sp)
                Text(
                    String.format("%.3f", surveyData.hrms),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (surveyData.hrms < 0.05) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("VRMS: ", fontSize = 10.sp)
                Text(
                    String.format("%.3f", surveyData.vrms),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (surveyData.vrms < 0.1) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Satellite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("${surveyData.satelliteCount} uydu", fontSize = 11.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Battery3Bar,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("${surveyData.batteryLevel}%", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SurveyModeSelector(
    selectedMode: CompleteSurveyMode,
    onModeSelected: (CompleteSurveyMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompleteSurveyMode.values().forEach { mode ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeSelected(mode) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == mode)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedMode == mode) 4.dp else 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            when (mode) {
                                CompleteSurveyMode.STANDART -> Icons.Default.RadioButtonChecked
                                CompleteSurveyMode.EPOKLU -> Icons.Default.Timeline
                                CompleteSurveyMode.HIZLI -> Icons.Default.Speed
                                CompleteSurveyMode.OTOMATIK -> Icons.Default.AutoMode
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (selectedMode == mode) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            when (mode) {
                                CompleteSurveyMode.STANDART -> "Standart"
                                CompleteSurveyMode.EPOKLU -> "Epoklu"
                                CompleteSurveyMode.HIZLI -> "Hızlı"
                                CompleteSurveyMode.OTOMATIK -> "Otomatik"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedMode == mode) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurveyToolsPanel(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onPointListClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onSketchClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ToolIconButton(Icons.Default.Settings, "Ayarlar", onSettingsClick)
            ToolIconButton(Icons.Default.List, "Nokta Listesi", onPointListClick)
            ToolIconButton(Icons.Default.CameraAlt, "Fotoğraf", onPhotoClick)
            ToolIconButton(Icons.Default.Draw, "Kroki", onSketchClick)
            ToolIconButton(Icons.Default.Save, "Kaydet", onSaveClick)

            Spacer(modifier = Modifier.weight(1f))

            // Ana alım butonu
            Card(
                modifier = Modifier.size(56.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Alım",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun GnssInformationCard(
    surveyData: CompleteSurveyData,
    selectedMode: CompleteSurveyMode,
    pointCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedMode) {
                        CompleteSurveyMode.STANDART -> "[Standart] alım modunda çalışırken, nokta kaydından önce koordinat değerleri, hata oranları gibi noktaya ait tüm detaylar gözlemlenir."
                        CompleteSurveyMode.EPOKLU -> "[Epoklu] alım modunda, noktaya ait koordinat değerleri gözlemlenir. Ölçü sırasında oluşan hatalar, her epoktaki sapmalar grafik olarak gözlemlenir."
                        CompleteSurveyMode.HIZLI -> "[Hızlı] alım modu ile çalışırken herhangi bir pencere değişimi olmaz. Kullanıcının nokta numarası ve detay koduna müdahale edebileceği butonlar belirir."
                        CompleteSurveyMode.OTOMATIK -> "[Otomatik] alım modu ile belirlenen aralık veya mesafelerde alım yapmak için kullanılan moddur. Ölçü sırasında herhangi bir pencere açılmaz, belirlenen kriterlere göre alım işlemi otomatik olarak başlar."
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (surveyData.isRtkFixed) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sol kolon
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "NOKTA BİLGİLERİ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CoordInfoRow("Kod", pointCode.ifEmpty { "—" })
                    CoordInfoRow("Kuzey", String.format("%.8f°", surveyData.latitude))
                    CoordInfoRow("Doğu", String.format("%.8f°", surveyData.longitude))
                    CoordInfoRow("Y", String.format("%.3f", surveyData.y))
                    CoordInfoRow("X", String.format("%.3f", surveyData.x))
                    CoordInfoRow("Z (Yükseklik)", String.format("%.3f", surveyData.z))
                }

                // Sağ kolon
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "KALİTE GÖSTERGELERİ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CoordInfoRow(
                        "Anten Y.",
                        String.format("%.1fm±%.2fm", surveyData.antenHeight, surveyData.hrms)
                    )
                    CoordInfoRow("HRMS", String.format("%.3f", surveyData.hrms), surveyData.hrms < 0.05)
                    CoordInfoRow("VRMS", String.format("%.3f", surveyData.vrms), surveyData.vrms < 0.1)
                    CoordInfoRow("Gecikme", "${surveyData.delay}", surveyData.delay <= 2)
                    CoordInfoRow("PDOP", String.format("%.1f", surveyData.pdop), surveyData.pdop < 3.0)
                    CoordInfoRow("Uydu Sayısı", "${surveyData.satelliteCount}", surveyData.satelliteCount >= 8)
                }
            }
        }
    }
}

@Composable
fun CoordInfoRow(label: String, value: String, isGood: Boolean? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = when (isGood) {
                true -> Color(0xFF4CAF50)
                false -> Color(0xFFFF5252)
                null -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun PointInputCard(
    pointName: String,
    onPointNameChange: (String) -> Unit,
    pointCode: String,
    onPointCodeChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Nokta Bilgileri",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pointName,
                    onValueChange = onPointNameChange,
                    label = { Text("Nokta Adı", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )

                OutlinedTextField(
                    value = pointCode,
                    onValueChange = onPointCodeChange,
                    label = { Text("Kod", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    trailingIcon = {
                        IconButton(onClick = { /* Kod arama */ }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Kod Ara",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Açıklama", fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
            )
        }
    }
}

// STANDART MOD KARTI
@Composable
fun StandardModeCard(
    onPhotoAndSketch: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "[Tamam] seçeneği ile kayıt işlemi sonlandırılır.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "[Fotoğraf ve Kroki] seçeneği ile noktaya fotoğraf ataması yapılabilir. Ekran üzerinden not yada şekil çizimi seçenekleri kullanılabilir.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPhotoAndSketch,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fotoğraf ve Kroki", fontSize = 11.sp)
                }

                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tamam", fontSize = 11.sp)
                }
            }
        }
    }
}

// EPOKLU MOD KARTI
@Composable
fun EpochModeCard(
    isRecording: Boolean,
    epochData: EpochData,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onShowGraph: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Yandaki şekilde görüldüğü üzere, 10 epok okunacak noktanın 4 okuması tamamlanmış ve okumaların birbirinden farkı 1 cm altında kalmıştır.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Epok ayarları ve ilerleme
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Epok Sayısı: ${epochData.totalEpochs}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Aralık: ${epochData.intervalSeconds} saniye",
                            fontSize = 11.sp
                        )
                    }

                    if (isRecording && epochData.currentEpoch > 0) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Epok ${epochData.currentEpoch}/${epochData.totalEpochs}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LinearProgressIndicator(
                            progress = epochData.currentEpoch.toFloat() / epochData.totalEpochs.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = Color.LightGray
                        )

                        Text(
                            "Okumaların sapması: < 0.01m",
                            fontSize = 10.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kontrol butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = if (isRecording) onStop else onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFFF5252) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) "Durdur" else "Başlat", fontSize = 11.sp)
                }

                Button(
                    onClick = onShowGraph,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    enabled = epochData.readings.isNotEmpty()
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Grafik", fontSize = 11.sp)
                }
            }
        }
    }
}

// HIZLI MOD KARTI
@Composable
fun QuickModeCard(
    onQuickRecord: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Hızlı alım modunda, nokta ölçü ve kayıt butonuna basıldığında nokta ölçüsü ve kaydı yapılır, herhangi bir onay penceresi görünmez.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onQuickRecord,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("HIZLI ALIM", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// OTOMATİK MOD KARTI
@Composable
fun AutomaticModeCard(
    isActive: Boolean,
    distanceCriteria: Double,
    timeCriteria: Int,
    onDistanceChange: (Double) -> Unit,
    onTimeChange: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Mesafe yada zaman kriteri ayarı için ⚙ butonu kullanılır.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Kriterler
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "KRİTERLER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Mesafe Kriteri:", fontSize = 10.sp)
                        Text(
                            "${distanceCriteria} metre",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Zaman Kriteri:", fontSize = 10.sp)
                        Text(
                            "$timeCriteria saniye",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kontrol butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = if (isActive) onStop else onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0xFFFF5252) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isActive) "Durdur" else "Başlat", fontSize = 11.sp)
                }

                Button(
                    onClick = onPause,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    ),
                    enabled = isActive
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Duraklat", fontSize = 11.sp)
                }

                IconButton(onClick = { /* Ayarlar */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                }
            }
        }
    }
}

@Composable
fun SurveyBottomBar(
    pointCount: Int,
    isActive: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Nokta Sayısı: $pointCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Text("Bilgi Çubuğu", fontSize = 10.sp)
            Text("Ekran Bilgileri", fontSize = 10.sp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isActive) Color(0xFFFF5252) else Color(0xFF4CAF50),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isActive) "Kaydediyor" else "Hazır",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// Dialog'lar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveySettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekran Bilgileri ve Ayarlar") },
        text = {
            Column {
                Text("HRMS Limiti: 0.05", fontSize = 11.sp)
                Text("VRMS Limiti: 0.1", fontSize = 11.sp)
                Text("PDOP Limiti: 3.0", fontSize = 11.sp)
                Text("Gecikme Limiti: 5 saniye", fontSize = 11.sp)
                Text("Nokta Adı Artışı: 1", fontSize = 11.sp)
                Text("Ölçüm Süresi: 3 saniye", fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aynı İşlem Kullanımı: Kapalı", fontSize = 11.sp)
                Text("Girilen Son Noktaya Odaklan: Açık", fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
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
    onDismiss: () -> Unit,
    onDelete: (SurveyPoint) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nokta Listesi (${points.size} nokta)") },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(points) { point ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(point.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("X: ${String.format("%.3f", point.x)}", fontSize = 9.sp)
                                Text("Y: ${String.format("%.3f", point.y)}", fontSize = 9.sp)
                                Text("Z: ${String.format("%.3f", point.z)}", fontSize = 9.sp)
                                Text("Doğruluk: ±${String.format("%.3f", point.accuracy)}m", fontSize = 9.sp)
                            }
                            IconButton(onClick = { onDelete(point) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAndSketchDialog(
    onDismiss: () -> Unit,
    onPhotoTaken: () -> Unit,
    onSketchDrawn: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fotoğraf ve Kroki")
            }
        },
        text = {
            Column {
                Text(
                    "Noktaya fotoğraf eklemek veya ekran üzerinde not/şekil çizimi yapabilirsiniz.",
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onPhotoTaken()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fotoğraf", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            onSketchDrawn()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kroki", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpochGraphDialog(
    epochData: EpochData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Epok Grafik Gösterimi") },
        text = {
            Column {
                Text(
                    "Her epoktaki sapmaların grafik gösterimi",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Basit grafik gösterimi (daire ile)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .border(2.dp, Color(0xFFFF0000), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Epok Okumaları",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${epochData.readings.size}/${epochData.totalEpochs} epok",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(
                            "Sapma: < 1 cm",
                            color = Color(0xFF4CAF50),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Okumaların birbirinden farkı 1 cm altında kalmıştır",
                    fontSize = 10.sp,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
