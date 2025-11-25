package com.example.tugis3.ui.analysis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class MeasurementAnalysisActivity : ComponentActivity() {
    private val vm: MeasurementAnalysisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                MeasurementAnalysisScreen(
                    vm = vm,
                    onBack = { finish() }
                )
            }
        }
    }
}

@HiltViewModel
class MeasurementAnalysisViewModel @Inject constructor(
    private val pointRepo: SurveyPointRepository
) : ViewModel() {

    val allPoints = pointRepo.observeAllPoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val statistics: StateFlow<MeasurementStats> = allPoints
        .map { points -> calculateStatistics(points) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeasurementStats())

    private fun calculateStatistics(points: List<SurveyPointEntity>): MeasurementStats {
        if (points.isEmpty()) return MeasurementStats()

        val hrmsValues = points.mapNotNull { it.hrms }
        val vrmsValues = points.mapNotNull { it.vrms }
        val pdopValues = points.mapNotNull { it.pdop }
        val satellites = points.mapNotNull { it.satellites }

        return MeasurementStats(
            totalPoints = points.size,
            avgHrms = if (hrmsValues.isNotEmpty()) hrmsValues.average() else 0.0,
            maxHrms = hrmsValues.maxOrNull() ?: 0.0,
            minHrms = hrmsValues.minOrNull() ?: 0.0,
            avgVrms = if (vrmsValues.isNotEmpty()) vrmsValues.average() else 0.0,
            maxVrms = vrmsValues.maxOrNull() ?: 0.0,
            minVrms = vrmsValues.minOrNull() ?: 0.0,
            avgPdop = if (pdopValues.isNotEmpty()) pdopValues.average() else 0.0,
            maxPdop = pdopValues.maxOrNull() ?: 0.0,
            minPdop = pdopValues.minOrNull() ?: 0.0,
            avgSatellites = if (satellites.isNotEmpty()) satellites.average().toInt() else 0,
            goodAccuracyCount = hrmsValues.count { it < 0.05 },
            fairAccuracyCount = hrmsValues.count { it in 0.05..0.1 },
            poorAccuracyCount = hrmsValues.count { it > 0.1 }
        )
    }
}

data class MeasurementStats(
    val totalPoints: Int = 0,
    val avgHrms: Double = 0.0,
    val maxHrms: Double = 0.0,
    val minHrms: Double = 0.0,
    val avgVrms: Double = 0.0,
    val maxVrms: Double = 0.0,
    val minVrms: Double = 0.0,
    val avgPdop: Double = 0.0,
    val maxPdop: Double = 0.0,
    val minPdop: Double = 0.0,
    val avgSatellites: Int = 0,
    val goodAccuracyCount: Int = 0,
    val fairAccuracyCount: Int = 0,
    val poorAccuracyCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementAnalysisScreen(
    vm: MeasurementAnalysisViewModel,
    onBack: () -> Unit
) {
    val allPoints by vm.allPoints.collectAsState()
    val stats by vm.statistics.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ölçüm Analizi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            // Tab Bar
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.BarChart, null) },
                    text = { Text("İstatistikler") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, null) },
                    text = { Text("Detaylar") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Info, null) },
                    text = { Text("Rapor") }
                )
            }

            when (selectedTab) {
                0 -> StatisticsTab(stats)
                1 -> DetailsTab(allPoints)
                2 -> ReportTab(stats)
            }
        }
    }
}

@Composable
private fun StatisticsTab(stats: MeasurementStats) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Genel Bilgiler
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("GENEL BİLGİLER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatBox("Toplam Nokta", "${stats.totalPoints}", MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        StatBox("Ortalama Uydu", "${stats.avgSatellites}", MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        item {
            // HRMS İstatistikleri
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("HRMS (Yatay Doğruluk)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    StatRow("Ortalama", String.format("%.4f m", stats.avgHrms))
                    StatRow("Maksimum", String.format("%.4f m", stats.maxHrms))
                    StatRow("Minimum", String.format("%.4f m", stats.minHrms))

                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(12.dp))

                    Text("Doğruluk Dağılımı", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))

                    ProgressBar(
                        label = "İyi (< 0.05 m)",
                        count = stats.goodAccuracyCount,
                        total = stats.totalPoints,
                        color = Color(0xFF4CAF50)
                    )
                    ProgressBar(
                        label = "Orta (0.05-0.1 m)",
                        count = stats.fairAccuracyCount,
                        total = stats.totalPoints,
                        color = Color(0xFFFF9800)
                    )
                    ProgressBar(
                        label = "Zayıf (> 0.1 m)",
                        count = stats.poorAccuracyCount,
                        total = stats.totalPoints,
                        color = Color(0xFFFF5252)
                    )
                }
            }
        }

        item {
            // VRMS İstatistikleri
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("VRMS (Dikey Doğruluk)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    StatRow("Ortalama", String.format("%.4f m", stats.avgVrms))
                    StatRow("Maksimum", String.format("%.4f m", stats.maxVrms))
                    StatRow("Minimum", String.format("%.4f m", stats.minVrms))
                }
            }
        }

        item {
            // PDOP İstatistikleri
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("PDOP (Konum Dilüsyonu)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    StatRow("Ortalama", String.format("%.1f", stats.avgPdop))
                    StatRow("Maksimum", String.format("%.1f", stats.maxPdop))
                    StatRow("Minimum", String.format("%.1f", stats.minPdop))

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "PDOP < 3.0 ideal, 3.0-5.0 iyi, > 5.0 zayıf",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsTab(points: List<SurveyPointEntity>) {
    if (points.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Veri bulunmamaktadır")
        }
    } else {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(points, key = { it.id }) { point ->
                PointDetailCard(point)
            }
        }
    }
}

@Composable
private fun ReportTab(stats: MeasurementStats) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("ÖLÇÜM RAPORu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))

                    ReportSection(
                        title = "GENEL BİLGİLER",
                        items = listOf(
                            "Toplam Ölçülen Nokta" to "${stats.totalPoints}",
                            "İyi Doğruluk Noktaları" to "${stats.goodAccuracyCount} (${if(stats.totalPoints > 0) (stats.goodAccuracyCount * 100 / stats.totalPoints) else 0}%)",
                            "Orta Doğruluk Noktaları" to "${stats.fairAccuracyCount} (${if(stats.totalPoints > 0) (stats.fairAccuracyCount * 100 / stats.totalPoints) else 0}%)",
                            "Zayıf Doğruluk Noktaları" to "${stats.poorAccuracyCount} (${if(stats.totalPoints > 0) (stats.poorAccuracyCount * 100 / stats.totalPoints) else 0}%)"
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    ReportSection(
                        title = "DOĞRULUK ANALİZİ",
                        items = listOf(
                            "Ortalama HRMS" to String.format("%.4f m", stats.avgHrms),
                            "Maksimum HRMS" to String.format("%.4f m", stats.maxHrms),
                            "Ortalama VRMS" to String.format("%.4f m", stats.avgVrms),
                            "Maksimum VRMS" to String.format("%.4f m", stats.maxVrms)
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    ReportSection(
                        title = "KONUMLANDıRMA KALİTESİ",
                        items = listOf(
                            "Ortalama PDOP" to String.format("%.1f", stats.avgPdop),
                            "Maksimum PDOP" to String.format("%.1f", stats.maxPdop),
                            "Ortalama Uydu Sayısı" to "${stats.avgSatellites}"
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Rapor: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(java.util.Date())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatBox(label: String, value: String, color: Color) {
    Column(
        Modifier
            .weight(1f)
            .background(color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProgressBar(label: String, count: Int, total: Int, color: Color) {
    val percentage = if (total > 0) count.toFloat() / total else 0f
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("$count / $total (${(percentage * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(
            progress = percentage,
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
    }
}

@Composable
private fun PointDetailCard(point: SurveyPointEntity) {
    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(point.name, fontWeight = FontWeight.Bold)
                    point.code?.let {
                        Text("Kod: $it", style = MaterialTheme.typography.labelSmall)
                    }
                }
                point.hrms?.let {
                    val statusColor = when {
                        it < 0.05 -> Color(0xFF4CAF50)
                        it < 0.1 -> Color(0xFFFF9800)
                        else -> Color(0xFFFF5252)
                    }
                    Box(
                        Modifier
                            .background(statusColor.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small)
                            .padding(6.dp)
                    ) {
                        Text(
                            String.format("%.4f m", it),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                point.northing?.let {
                    Text("K: ${String.format("%.2f", it)}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
                point.easting?.let {
                    Text("D: ${String.format("%.2f", it)}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
                point.elevation?.let {
                    Text("Y: ${String.format("%.2f", it)}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun ReportSection(title: String, items: List<Pair<String, String>>) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        items.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
