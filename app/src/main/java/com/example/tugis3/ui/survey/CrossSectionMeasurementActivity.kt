package com.example.tugis3.ui.survey

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Kesit Plan Aplikasyonu - Kullanım kılavuzuna göre geliştirilmiş
 * Hat boyunca kesit ölçümleri yapılmasını sağlar
 */
@AndroidEntryPoint
class CrossSectionMeasurementActivity : ComponentActivity() {

    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null
    private var satelliteCount = 0

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // İzin kontrolü
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            startLocationUpdates()
        }

        setContent {
            Tugis3Theme {
                CrossSectionScreen(
                    onBack = { finish() },
                    getLocation = { currentLocation },
                    getSatelliteCount = { satelliteCount }
                )
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // 1 saniye
                    0f,   // 0 metre
                    locationListener
                )

                // Son bilinen konumu al
                currentLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(locationListener)
    }
}

// Veri modelleri
data class CrossSectionPoint(
    val station: Double,        // İstasyon (Km veya m)
    val offset: Double,         // Ofset (sol(-) / sağ(+))
    val elevation: Double,      // Kot
    val latitude: Double = 0.0, // GPS Enlem
    val longitude: Double = 0.0,// GPS Boylam
    val accuracy: Float = 0f,   // GPS Hassasiyet (m)
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = ""
)

data class CrossSectionLine(
    val name: String,
    val startStation: Double,
    val endStation: Double,
    val interval: Double,       // Kesit aralığı
    val points: MutableList<CrossSectionPoint> = mutableListOf()
)

data class CrossSectionUiState(
    val currentLine: CrossSectionLine? = null,
    val currentStation: Double = 0.0,
    val currentOffset: Double = 0.0,
    val currentElevation: Double = 0.0,
    val measurementPoints: List<CrossSectionPoint> = emptyList(),
    val isRecording: Boolean = false,
    val autoMeasureEnabled: Boolean = false,
    val leftOffsetLimit: Double = -20.0,
    val rightOffsetLimit: Double = 20.0,
    val offsetInterval: Double = 2.0,
    val gpsAccuracy: Double = 5.0,
    val satelliteCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrossSectionScreen(
    onBack: () -> Unit,
    getLocation: () -> Location?,
    getSatelliteCount: () -> Int
) {
    var uiState by remember { mutableStateOf(CrossSectionUiState()) }
    var showStationSelector by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPointList by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // GPS verilerini güncelle
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        while (true) {
            val location = getLocation()
            if (location != null) {
                uiState = uiState.copy(
                    currentElevation = location.altitude,
                    gpsAccuracy = location.accuracy.toDouble() / 100.0, // metre -> cm
                    satelliteCount = getSatelliteCount()
                )
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    // Örnek hat oluştur
    LaunchedEffect(Unit) {
        if (uiState.currentLine == null) {
            uiState = uiState.copy(
                currentLine = CrossSectionLine(
                    name = "Ana Kanal Kesiti",
                    startStation = 0.0,
                    endStation = 500.0,
                    interval = 20.0
                ),
                currentStation = 0.0,
                currentOffset = 0.0,
                currentElevation = 100.5
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kesit Aplikasyonu")
                        uiState.currentLine?.let { line ->
                            Text(
                                text = line.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    // Export butonu
                    IconButton(
                        onClick = { showExportDialog = true },
                        enabled = uiState.measurementPoints.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Save, "Dışa Aktar")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Ayarlar")
                    }
                    BadgedBox(
                        badge = {
                            if (uiState.measurementPoints.isNotEmpty()) {
                                Badge {
                                    Text("${uiState.measurementPoints.size}")
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { showPointList = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, "Noktalar")
                        }
                    }
                    IconButton(onClick = { showStationSelector = true }) {
                        Icon(Icons.Default.LocationOn, "İstasyon Seç")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Kayıt butonu
                FloatingActionButton(
                    onClick = {
                        val location = getLocation()
                        val newPoint = CrossSectionPoint(
                            station = uiState.currentStation,
                            offset = uiState.currentOffset,
                            elevation = uiState.currentElevation,
                            latitude = location?.latitude ?: 0.0,
                            longitude = location?.longitude ?: 0.0,
                            accuracy = location?.accuracy ?: 0f
                        )
                        uiState = uiState.copy(
                            measurementPoints = uiState.measurementPoints + newPoint
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.AddCircle, "Nokta Ekle")
                }

                // Otomatik ölçüm butonu
                ExtendedFloatingActionButton(
                    onClick = {
                        uiState = uiState.copy(
                            autoMeasureEnabled = !uiState.autoMeasureEnabled
                        )
                    },
                    icon = {
                        Icon(
                            if (uiState.autoMeasureEnabled) Icons.Default.Stop
                            else Icons.Default.PlayArrow,
                            null
                        )
                    },
                    text = {
                        Text(if (uiState.autoMeasureEnabled) "Durdur" else "Oto Ölçüm")
                    },
                    containerColor = if (uiState.autoMeasureEnabled)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hat Bilgisi
            item {
                CrossSectionLineInfoCard(uiState.currentLine)
            }

            // Mevcut Konum Bilgisi
            item {
                CurrentPositionCard(uiState)
            }

            // Kesit Görünümü
            item {
                CrossSectionVisualizationCard(uiState)
            }

            // İstatistikler
            if (uiState.measurementPoints.isNotEmpty()) {
                item {
                    CrossSectionStatsCard(uiState.measurementPoints)
                }
            }
        }
    }

    // İstasyon seçici dialog
    if (showStationSelector) {
        StationSelectorDialog(
            line = uiState.currentLine,
            currentStation = uiState.currentStation,
            onStationSelect = { station ->
                uiState = uiState.copy(currentStation = station)
                showStationSelector = false
            },
            onDismiss = { showStationSelector = false }
        )
    }

    // Ayarlar dialog
    if (showSettings) {
        CrossSectionSettingsDialog(
            uiState = uiState,
            onSettingsChange = { newState ->
                uiState = newState
            },
            onDismiss = { showSettings = false }
        )
    }

    // Nokta listesi dialog
    if (showPointList) {
        PointListDialog(
            points = uiState.measurementPoints,
            onDeletePoint = { point ->
                uiState = uiState.copy(
                    measurementPoints = uiState.measurementPoints - point
                )
            },
            onDismiss = { showPointList = false }
        )
    }

    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            line = uiState.currentLine,
            points = uiState.measurementPoints,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun CrossSectionLineInfoCard(line: CrossSectionLine?) {
    Card {
        if (line != null) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Kesit Hattı Bilgisi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = line.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CrossSectionInfoItem("Başlangıç", "Km ${String.format(Locale.getDefault(), "%.3f", line.startStation)}")
                    CrossSectionInfoItem("Bitiş", "Km ${String.format(Locale.getDefault(), "%.3f", line.endStation)}")
                    CrossSectionInfoItem("Aralık", "${String.format(Locale.getDefault(), "%.1f", line.interval)} m")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lütfen bir kesit hattı oluşturun",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun CurrentPositionCard(uiState: CrossSectionUiState) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Mevcut Konum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CrossSectionInfoItem(
                    "İstasyon",
                    String.format(Locale.getDefault(), "%.3f", uiState.currentStation)
                )
                CrossSectionInfoItem(
                    "Ofset",
                    String.format(Locale.getDefault(), "%.2f m %s",
                        abs(uiState.currentOffset),
                        if (uiState.currentOffset >= 0) "SAĞ" else "SOL"
                    )
                )
                CrossSectionInfoItem(
                    "Kot",
                    String.format(Locale.getDefault(), "%.3f", uiState.currentElevation)
                )
            }

            // GPS Bilgisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Satellite,
                        contentDescription = null,
                        tint = if (uiState.gpsAccuracy < 3.0)
                            Color.Green else MaterialTheme.colorScheme.onSurface
                    )
                    Text("${uiState.satelliteCount} uydu")
                }

                Text(
                    text = "Hassasiyet: ${String.format(Locale.getDefault(), "%.1f", uiState.gpsAccuracy)} cm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.gpsAccuracy < 3.0) Color.Green else Color(0xFFFFA500)
                )
            }
        }
    }
}

@Composable
private fun CrossSectionVisualizationCard(uiState: CrossSectionUiState) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Kesit Görünümü",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "İstasyon: ${String.format(Locale.getDefault(), "%.3f", uiState.currentStation)}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Kesit çizimi
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                val padding = 50f
                val width = size.width - 2 * padding
                val height = size.height - 2 * padding

                // Mevcut istasyondaki noktaları filtrele
                val stationPoints = uiState.measurementPoints
                    .filter { abs(it.station - uiState.currentStation) < 0.5 }
                    .sortedBy { it.offset }

                if (stationPoints.isNotEmpty()) {
                    val minOffset = stationPoints.minOf { it.offset }.coerceAtMost(uiState.leftOffsetLimit)
                    val maxOffset = stationPoints.maxOf { it.offset }.coerceAtLeast(uiState.rightOffsetLimit)
                    val minElev = stationPoints.minOf { it.elevation } - 2.0
                    val maxElev = stationPoints.maxOf { it.elevation } + 2.0

                    val scaleX = width / (maxOffset - minOffset)
                    val scaleY = height / (maxElev - minElev)

                    fun toScreenX(offset: Double): Float =
                        padding + ((offset - minOffset) * scaleX).toFloat()
                    fun toScreenY(elev: Double): Float =
                        size.height - padding - ((elev - minElev) * scaleY).toFloat()

                    // Zemin çizgisini çiz
                    val groundPath = Path()
                    stationPoints.forEachIndexed { index, point ->
                        val x = toScreenX(point.offset)
                        val y = toScreenY(point.elevation)
                        if (index == 0) {
                            groundPath.moveTo(x, y)
                        } else {
                            groundPath.lineTo(x, y)
                        }
                    }
                    drawPath(groundPath, Color(0xFF2196F3), style = Stroke(width = 3f))

                    // Ölçüm noktalarını çiz
                    stationPoints.forEach { point ->
                        val x = toScreenX(point.offset)
                        val y = toScreenY(point.elevation)
                        drawCircle(
                            color = Color.Red,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }

                    // Merkez çizgisi (ofset = 0)
                    val centerX = toScreenX(0.0)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(centerX, padding),
                        end = Offset(centerX, size.height - padding),
                        strokeWidth = 2f
                    )
                } else {
                    // Henüz nokta yoksa boş kesit göster
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(padding, size.height / 2),
                        end = Offset(size.width - padding, size.height / 2),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

@Composable
private fun CrossSectionStatsCard(points: List<CrossSectionPoint>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "İstatistikler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CrossSectionInfoItem("Toplam Nokta", "${points.size}")
                CrossSectionInfoItem("Min Kot", String.format(Locale.getDefault(), "%.2f", points.minOfOrNull { it.elevation } ?: 0.0))
                CrossSectionInfoItem("Max Kot", String.format(Locale.getDefault(), "%.2f", points.maxOfOrNull { it.elevation } ?: 0.0))
            }

            val groupedByStation = points.groupBy { it.station }
            Text(
                text = "${groupedByStation.size} istasyonda ölçüm yapıldı",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationSelectorDialog(
    line: CrossSectionLine?,
    currentStation: Double,
    onStationSelect: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    if (line == null) {
        onDismiss()
        return
    }

    val stations = (line.startStation.toInt()..line.endStation.toInt()
        step line.interval.toInt())
        .map { it.toDouble() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("İstasyon Seç") },
        text = {
            LazyColumn {
                items(stations) { station ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        onClick = { onStationSelect(station) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (abs(station - currentStation) < 0.1)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Km ${String.format(Locale.getDefault(), "%.3f", station)}")
                            if (abs(station - currentStation) < 0.1) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrossSectionSettingsDialog(
    uiState: CrossSectionUiState,
    onSettingsChange: (CrossSectionUiState) -> Unit,
    onDismiss: () -> Unit
) {
    var leftLimit by remember { mutableStateOf(abs(uiState.leftOffsetLimit).toString()) }
    var rightLimit by remember { mutableStateOf(uiState.rightOffsetLimit.toString()) }
    var interval by remember { mutableStateOf(uiState.offsetInterval.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kesit Ayarları") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = leftLimit,
                    onValueChange = { leftLimit = it },
                    label = { Text("Sol Ofset Limiti (m)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rightLimit,
                    onValueChange = { rightLimit = it },
                    label = { Text("Sağ Ofset Limiti (m)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it },
                    label = { Text("Ofset Aralığı (m)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newState = uiState.copy(
                    leftOffsetLimit = -(leftLimit.toDoubleOrNull() ?: 20.0),
                    rightOffsetLimit = rightLimit.toDoubleOrNull() ?: 20.0,
                    offsetInterval = interval.toDoubleOrNull() ?: 2.0
                )
                onSettingsChange(newState)
                onDismiss()
            }) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointListDialog(
    points: List<CrossSectionPoint>,
    onDeletePoint: (CrossSectionPoint) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ölçüm Noktaları (${points.size})") },
        text = {
            LazyColumn {
                items(points.sortedByDescending { it.timestamp }) { point ->
                    ListItem(
                        headlineContent = {
                            Text("İst: ${String.format(Locale.getDefault(), "%.3f", point.station)}")
                        },
                        supportingContent = {
                            Text(
                                "Ofset: ${String.format(Locale.getDefault(), "%.2f", point.offset)} | " +
                                "Kot: ${String.format(Locale.getDefault(), "%.3f", point.elevation)}"
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onDeletePoint(point) }) {
                                Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    HorizontalDivider()
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

@Composable
private fun CrossSectionInfoItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// Export Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDialog(
    line: CrossSectionLine?,
    points: List<CrossSectionPoint>,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedFormat by remember { mutableStateOf("CSV") }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dışa Aktar") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${points.size} nokta kaydedilecek",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Format seçimi
                Text(
                    text = "Format Seçin:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == "CSV",
                        onClick = { selectedFormat = "CSV" },
                        label = { Text("CSV") }
                    )
                    FilterChip(
                        selected = selectedFormat == "HDM",
                        onClick = { selectedFormat = "HDM" },
                        label = { Text("HDM") }
                    )
                    FilterChip(
                        selected = selectedFormat == "TXT",
                        onClick = { selectedFormat = "TXT" },
                        label = { Text("TXT") }
                    )
                }

                // Export durumu
                exportStatus?.let { status ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (status.contains("Başarılı"))
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = when (selectedFormat) {
                        "CSV" -> exportToCSV(context, line, points)
                        "HDM" -> exportToHDM(context, line, points)
                        "TXT" -> exportToTXT(context, line, points)
                        else -> "Bilinmeyen format"
                    }
                    exportStatus = result
                }
            ) {
                Text("Dışa Aktar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

// Export fonksiyonları
@Suppress("UNUSED_PARAMETER")
private fun exportToCSV(context: Context, line: CrossSectionLine?, points: List<CrossSectionPoint>): String {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "kesit_${line?.name?.replace(" ", "_") ?: "export"}_$timestamp.csv"

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val tugisDir = File(documentsDir, "TUGIS3/CrossSections")
        tugisDir.mkdirs()

        val file = File(tugisDir, fileName)

        file.bufferedWriter().use { writer ->
            // Header
            writer.write("Station,Offset,Elevation,Latitude,Longitude,Accuracy,Timestamp\n")

            // Data
            points.forEach { point ->
                writer.write("${point.station},${point.offset},${point.elevation},${point.latitude},${point.longitude},${point.accuracy},${point.timestamp}\n")
            }
        }

        return "Başarılı! Dosya: ${file.absolutePath}"
    } catch (e: Exception) {
        return "Hata: ${e.message}"
    }
}

@Suppress("UNUSED_PARAMETER")
private fun exportToHDM(context: Context, line: CrossSectionLine?, points: List<CrossSectionPoint>): String {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "kesit_${line?.name?.replace(" ", "_") ?: "export"}_$timestamp.hdm"

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val tugisDir = File(documentsDir, "TUGIS3/CrossSections")
        tugisDir.mkdirs()

        val file = File(tugisDir, fileName)

        file.bufferedWriter().use { writer ->
            // HDM Header (SurPad format)
            writer.write("[Header]\n")
            writer.write("Version=4.2\n")
            writer.write("ProjectName=${line?.name ?: "Kesit"}\n")
            writer.write("CreateDate=${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.write("\n[CrossSection]\n")
            writer.write("LineName=${line?.name ?: "Unnamed"}\n")
            writer.write("StartStation=${line?.startStation ?: 0.0}\n")
            writer.write("EndStation=${line?.endStation ?: 0.0}\n")
            writer.write("Interval=${line?.interval ?: 20.0}\n")
            writer.write("\n[Points]\n")

            // Data
            points.forEach { point ->
                writer.write("${point.station},${point.offset},${point.elevation},${point.latitude},${point.longitude},${point.accuracy}\n")
            }
        }

        return "Başarılı! Dosya: ${file.absolutePath}"
    } catch (e: Exception) {
        return "Hata: ${e.message}"
    }
}

@Suppress("UNUSED_PARAMETER")
private fun exportToTXT(context: Context, line: CrossSectionLine?, points: List<CrossSectionPoint>): String {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "kesit_${line?.name?.replace(" ", "_") ?: "export"}_$timestamp.txt"

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val tugisDir = File(documentsDir, "TUGIS3/CrossSections")
        tugisDir.mkdirs()

        val file = File(tugisDir, fileName)

        file.bufferedWriter().use { writer ->
            writer.write("========================================\n")
            writer.write("KESİT PLAN APLİKASYONU - ÖLÇÜM RAPORU\n")
            writer.write("========================================\n\n")
            writer.write("Hat Adı: ${line?.name ?: "Belirtilmemiş"}\n")
            writer.write("Başlangıç İstasyon: ${line?.startStation ?: 0.0}\n")
            writer.write("Bitiş İstasyon: ${line?.endStation ?: 0.0}\n")
            writer.write("Aralık: ${line?.interval ?: 20.0} m\n")
            writer.write("Tarih: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.write("Toplam Nokta: ${points.size}\n\n")

            writer.write("----------------------------------------\n")
            writer.write("ÖLÇÜM NOKTALARI\n")
            writer.write("----------------------------------------\n\n")

            points.forEachIndexed { index, point ->
                writer.write("Nokta ${index + 1}:\n")
                writer.write("  İstasyon: ${String.format(Locale.getDefault(), "%.3f", point.station)}\n")
                writer.write("  Ofset: ${String.format(Locale.getDefault(), "%.2f", point.offset)} m\n")
                writer.write("  Kot: ${String.format(Locale.getDefault(), "%.3f", point.elevation)} m\n")
                writer.write("  Enlem: ${String.format(Locale.getDefault(), "%.8f", point.latitude)}°\n")
                writer.write("  Boylam: ${String.format(Locale.getDefault(), "%.8f", point.longitude)}°\n")
                writer.write("  Hassasiyet: ${String.format(Locale.getDefault(), "%.2f", point.accuracy)} m\n\n")
            }

            // İstatistikler
            if (points.isNotEmpty()) {
                writer.write("----------------------------------------\n")
                writer.write("İSTATİSTİKLER\n")
                writer.write("----------------------------------------\n\n")

                val minElev = points.minOf { it.elevation }
                val maxElev = points.maxOf { it.elevation }
                val avgElev = points.map { it.elevation }.average()

                writer.write("Minimum Kot: ${String.format(Locale.getDefault(), "%.3f", minElev)} m\n")
                writer.write("Maximum Kot: ${String.format(Locale.getDefault(), "%.3f", maxElev)} m\n")
                writer.write("Ortalama Kot: ${String.format(Locale.getDefault(), "%.3f", avgElev)} m\n")
                writer.write("Kot Farkı: ${String.format(Locale.getDefault(), "%.3f", maxElev - minElev)} m\n")
            }

            writer.write("\n========================================\n")
            writer.write("RAPOR SONU\n")
            writer.write("========================================\n")
        }

        return "Başarılı! Dosya: ${file.absolutePath}"
    } catch (e: Exception) {
        return "Hata: ${e.message}"
    }
}
