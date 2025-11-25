package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.gnss.model.FixType
import com.example.tugis3.ui.survey.model.SurveyPoint
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GraphicalSurveyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                GraphicalSurveyScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GraphicalSurveyScreen(onBack: () -> Unit) {
    var selectedPoints by remember { mutableStateOf(listOf<SurveyPoint>()) }
    var isRecording by remember { mutableStateOf(false) }
    var surveyMode by remember { mutableStateOf("Nokta Toplama") }

    // GNSS konumu al
    val pos by GnssPositionRepository.position.collectAsState()
    val lat = pos.lat
    val lng = pos.lon
    val fix = pos.fixType

    // Örnek noktalar
    val samplePoints = remember {
        listOf(
            SurveyPoint(id = "P001", name = "Başlangıç Noktası", lat = 40.123456, lng = 29.876543),
            SurveyPoint(id = "P002", name = "Ara Nokta", lat = 40.123556, lng = 29.876643),
            SurveyPoint(id = "P003", name = "Bitiş Noktası", lat = 40.123656, lng = 29.876743)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grafik Alım") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Görünüm ayarları */ }) {
                        Icon(Icons.Default.Visibility, contentDescription = "Görünüm")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Grafik Harita Alanı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawGraphicalSurvey(samplePoints, selectedPoints, lat, lng)
                    }

                    // Harita kontrolleri
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { /* Yakınlaştır */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Yakınlaştır", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { /* Uzaklaştır */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Uzaklaştır", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Survey Modu
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Survey Modu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mevcut Mod:")
                        AssistChip(
                            onClick = { },
                            label = { Text(surveyMode) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Otomatik Kayıt:")
                        Switch(
                            checked = isRecording,
                            onCheckedChange = { isRecording = it }
                        )
                    }
                }
            }

            // Mevcut Konum
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Mevcut Konum",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GPS Fix:")
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(when(fix) {
                                    FixType.RTK_FIX -> "RTK FIX"
                                    FixType.RTK_FLOAT -> "RTK FLOAT"
                                    else -> "GPS"
                                })
                            },
                            leadingIcon = {
                                Icon(
                                    when(fix) {
                                        FixType.RTK_FIX -> Icons.Default.CheckCircle
                                        FixType.RTK_FLOAT -> Icons.Default.Warning
                                        else -> Icons.Default.Cancel
                                    },
                                    contentDescription = null,
                                    tint = when(fix) {
                                        FixType.RTK_FIX -> Color.Green
                                        FixType.RTK_FLOAT -> Color.Yellow
                                        else -> Color.Red
                                    }
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Koordinat:")
                        Text("${lat?.let { "%.6f".format(it) } ?: "---"}, ${lng?.let { "%.6f".format(it) } ?: "---"}")
                    }
                }
            }

            // Nokta Listesi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Ölçülen Noktalar (${samplePoints.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    samplePoints.forEach { point ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(point.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${point.lat.format(6)}, ${point.lng.format(6)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { /* Nokta seç */ }) {
                                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Seç")
                            }
                        }
                    }
                }
            }

            // Kontrol Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Nokta ekle */ },
                    modifier = Modifier.weight(1f),
                    enabled = fix == FixType.RTK_FIX || fix == FixType.RTK_FLOAT
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nokta Ekle")
                }

                OutlinedButton(
                    onClick = { /* Survey bitir */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kaydet")
                }
            }
        }
    }
}

private fun DrawScope.drawGraphicalSurvey(
    points: List<SurveyPoint>,
    selectedPoints: List<SurveyPoint>,
    currentLat: Double?,
    currentLng: Double?
) {
    // Basit harita çizimi
    val centerX = size.width / 2
    val centerY = size.height / 2

    // Grid çizgileri
    for (i in 0..10) {
        val x = (size.width * i / 10)
        val y = (size.height * i / 10)
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Ölçülen noktaları çiz
    points.forEachIndexed { index, point ->
        val x = centerX + (index - 1) * 50f
        val y = centerY + (index - 1) * 30f

        drawCircle(
            color = Color.Blue,
            radius = 8.dp.toPx(),
            center = Offset(x, y)
        )
    }

    // Mevcut konumu çiz
    if (currentLat != null && currentLng != null) {
        drawCircle(
            color = Color.Red,
            radius = 10.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
