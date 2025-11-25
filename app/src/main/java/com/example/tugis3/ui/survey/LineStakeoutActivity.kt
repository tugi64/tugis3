package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.tugis3.model.LineStakeout
import com.example.tugis3.model.CADPoint
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.*
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hat Aplikasyonu (Line Stakeout) - Kullanım kılavuzu bölüm 6.5
 * İki nokta arasında hat çizerek aplikasyon yapmayı sağlar
 */
@AndroidEntryPoint
class LineStakeoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                LineStakeoutScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

// UI State data classes
data class LineStakeoutUiState(
    val currentLine: LineStakeout? = null,
    val currentPosition: CADPoint = CADPoint(0.0, 0.0, 0.0),
    val closestPointOnLine: CADPoint? = null,
    val distanceAlongLine: Double = 0.0,
    val offsetFromLine: Double = 0.0,
    val offsetDistance: Double = 0.0,  // Kullanıcının ayarladığı ofset
    val stationInterval: Double = 20.0,  // İstasyon aralığı (m)
    val isNavigating: Boolean = false,
    val targetStation: Double = 0.0,
    val gpsEnabled: Boolean = false,
    val accuracy: Double = 5.0,
    val satelliteCount: Int = 0,
    val availableLines: List<LineStakeout> = emptyList(),
    val completedStakeouts: List<LineStakeoutRecord> = emptyList()
)

data class LineStakeoutRecord(
    val station: Double,
    val offset: Double,
    val measuredPoint: CADPoint,
    val timestamp: Long = System.currentTimeMillis()
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineStakeoutScreen(
    viewModel: LineStakeoutViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLineSelector by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hat Aplikasyonu")
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
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Ayarlar")
                    }
                    IconButton(onClick = { showLineSelector = true }) {
                        Icon(Icons.Default.AddRoad, "Hat Seç")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.currentLine != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kayıt butonu
                    FloatingActionButton(
                        onClick = { viewModel.recordStakeout() },
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ) {
                        Icon(Icons.Default.SaveAlt, "Kaydet")
                    }

                    // Navigasyon butonu
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (uiState.isNavigating) {
                                viewModel.stopNavigation()
                            } else {
                                viewModel.navigateToStation(uiState.targetStation)
                            }
                        },
                        icon = {
                            Icon(
                                if (uiState.isNavigating) Icons.Default.Stop
                                else Icons.Default.Navigation,
                                null
                            )
                        },
                        text = {
                            Text(if (uiState.isNavigating) "Durdur" else "Başla")
                        },
                        containerColor = if (uiState.isNavigating)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
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
                LineInfoCard(uiState.currentLine)
            }

            // Mevcut Konum ve Hat Bilgisi
            item {
                if (uiState.currentLine != null) {
                    LinePositionCard(uiState)
                }
            }

            // Harita Görünümü
            item {
                LineMapView(uiState)
            }

            // İstasyon Listesi
            item {
                uiState.currentLine?.let { currentLine ->
                    StationListCard(
                        line = currentLine,
                        interval = uiState.stationInterval,
                        currentStation = uiState.distanceAlongLine,
                        onStationClick = { station ->
                            viewModel.navigateToStation(station)
                        }
                    )
                }
            }

            // Aplikasyon Geçmişi
            if (uiState.completedStakeouts.isNotEmpty()) {
                item {
                    StakeoutHistoryCard(uiState.completedStakeouts)
                }
            }
        }
    }

    // Hat Seçici Dialog
    if (showLineSelector) {
        LineSelectorDialog(
            lines = uiState.availableLines,
            onLineSelect = { line ->
                viewModel.selectLine(line)
                showLineSelector = false
            },
            onDismiss = { showLineSelector = false }
        )
    }

    // Ayarlar Dialog
    if (showSettings) {
        LineSettingsDialog(
            offsetDistance = uiState.offsetDistance,
            stationInterval = uiState.stationInterval,
            onOffsetChange = { viewModel.setOffsetDistance(it) },
            onIntervalChange = { viewModel.setStationInterval(it) },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun LineInfoCard(line: LineStakeout?) {
    Card {
        if (line != null) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hat Bilgisi",
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PointInfoItem("Uzunluk", String.format(Locale.getDefault(), "%.2f m", line.getLength()))
                    PointInfoItem("Başlangıç", "%.2f, %.2f".format(Locale.getDefault(), line.startPoint.x, line.startPoint.y))
                    PointInfoItem("Bitiş", "%.2f, %.2f".format(Locale.getDefault(), line.endPoint.x, line.endPoint.y))
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
                    text = "Lütfen bir hat seçin",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LinePositionCard(uiState: LineStakeoutUiState) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Hat Üzerinde Konum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                PointInfoItem(
                    label = "İstasyon",
                    value = String.format(Locale.getDefault(), "%.2f m", uiState.distanceAlongLine)
                )
                PointInfoItem(
                    label = "Ofset",
                    value = String.format(Locale.getDefault(), "%.2f m", uiState.offsetFromLine)
                )
                PointInfoItem(
                    label = "Hassasiyet",
                    value = String.format(Locale.getDefault(), "%.1f cm", uiState.accuracy)
                )
            }

            // Ofset yönü göstergesi
            val offsetDirection = if (uiState.offsetFromLine > 0) "SAĞ" else "SOL"
            Text(
                text = "Hatın $offsetDirection tarafındasınız",
                style = MaterialTheme.typography.bodyMedium,
                color = if (abs(uiState.offsetFromLine) < 0.5)
                    Color.Green else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LineMapView(uiState: LineStakeoutUiState) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Harita Görünümü",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                val line = uiState.currentLine ?: return@Canvas

                // Koordinat dönüşümü için ölçekleme
                val padding = 50f
                val width = size.width - 2 * padding
                val height = size.height - 2 * padding

                val minX = minOf(line.startPoint.x, line.endPoint.x, uiState.currentPosition.x)
                val maxX = maxOf(line.startPoint.x, line.endPoint.x, uiState.currentPosition.x)
                val minY = minOf(line.startPoint.y, line.endPoint.y, uiState.currentPosition.y)
                val maxY = maxOf(line.startPoint.y, line.endPoint.y, uiState.currentPosition.y)

                val scaleX = if (maxX - minX > 0) (width / (maxX - minX)).toFloat() else 1f
                val scaleY = if (maxY - minY > 0) (height / (maxY - minY)).toFloat() else 1f
                val scale = min(scaleX, scaleY)

                fun toScreenX(x: Double): Float = padding + ((x - minX) * scale).toFloat()
                fun toScreenY(y: Double): Float = size.height - padding - ((y - minY) * scale).toFloat()

                // Hat çiz
                drawLine(
                    color = Color.Blue,
                    start = Offset(toScreenX(line.startPoint.x), toScreenY(line.startPoint.y)),
                    end = Offset(toScreenX(line.endPoint.x), toScreenY(line.endPoint.y)),
                    strokeWidth = 4f
                )

                // Başlangıç ve bitiş noktaları
                drawCircle(
                    color = Color.Green,
                    radius = 8f,
                    center = Offset(toScreenX(line.startPoint.x), toScreenY(line.startPoint.y))
                )
                drawCircle(
                    color = Color.Red,
                    radius = 8f,
                    center = Offset(toScreenX(line.endPoint.x), toScreenY(line.endPoint.y))
                )

                // En yakın nokta
                uiState.closestPointOnLine?.let { closest ->
                    drawCircle(
                        color = Color.Yellow,
                        radius = 6f,
                        center = Offset(toScreenX(closest.x), toScreenY(closest.y))
                    )

                    // Mevcut konumdan hatta bağlantı çizgisi
                    drawLine(
                        color = Color.Gray,
                        start = Offset(toScreenX(closest.x), toScreenY(closest.y)),
                        end = Offset(toScreenX(uiState.currentPosition.x), toScreenY(uiState.currentPosition.y)),
                        strokeWidth = 2f
                    )
                }

                // Mevcut konum
                drawCircle(
                    color = Color.Cyan,
                    radius = 10f,
                    center = Offset(toScreenX(uiState.currentPosition.x), toScreenY(uiState.currentPosition.y))
                )
            }
        }
    }
}

@Composable
private fun StationListCard(
    line: LineStakeout,
    interval: Double,
    currentStation: Double,
    onStationClick: (Double) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "İstasyon Listesi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val stations = (0..line.getLength().toInt() step interval.toInt()).map { it.toDouble() }

            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stations) { station ->
                    StationItem(
                        station = station,
                        isCurrent = abs(station - currentStation) < interval / 2,
                        onClick = { onStationClick(station) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StationItem(
    station: Double,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "İstasyon ${String.format(Locale.getDefault(), "%.0f", station)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )

            if (isCurrent) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StakeoutHistoryCard(records: List<LineStakeoutRecord>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aplikasyon Geçmişi (${records.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            records.takeLast(5).forEach { record ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("İst: ${String.format(Locale.getDefault(), "%.1f", record.station)}")
                    Text("Ofset: ${String.format(Locale.getDefault(), "%.2f", record.offset)}")
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(record.timestamp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineSelectorDialog(
    lines: List<LineStakeout>,
    onLineSelect: (LineStakeout) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hat Seç") },
        text = {
            LazyColumn {
                items(lines) { line ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onLineSelect(line) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = line.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Uzunluk: ${String.format(Locale.getDefault(), "%.2f m", line.getLength())}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineSettingsDialog(
    offsetDistance: Double,
    stationInterval: Double,
    onOffsetChange: (Double) -> Unit,
    onIntervalChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var offset by remember { mutableStateOf(offsetDistance.toString()) }
    var interval by remember { mutableStateOf(stationInterval.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hat Ayarları") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = offset,
                    onValueChange = { offset = it },
                    label = { Text("Ofset Mesafesi (m)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it },
                    label = { Text("İstasyon Aralığı (m)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                offset.toDoubleOrNull()?.let { onOffsetChange(it) }
                interval.toDoubleOrNull()?.let { onIntervalChange(it) }
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

@Composable
private fun PointInfoItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

