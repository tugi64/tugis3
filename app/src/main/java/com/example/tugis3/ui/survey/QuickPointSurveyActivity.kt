@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.survey

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.gnss.model.FixType
import com.example.tugis3.data.db.dao.PointDao
import com.example.tugis3.data.db.entity.PointEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class QuickPointSurveyActivity : ComponentActivity() {

    @Inject
    lateinit var pointDao: PointDao

    private var currentProjectId = 1L // TODO: Aktif projeyi repository'den al

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                QuickPointSurveyScreen(
                    onBack = { finish() },
                    onSavePoint = { pointName, lat, lng, alt, fixType ->
                        savePointToDatabase(pointName, lat, lng, alt, fixType)
                    }
                )
            }
        }
    }

    private fun savePointToDatabase(pointName: String, lat: Double, lng: Double, alt: Double?, fixType: FixType?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val point = PointEntity(
                    id = 0, // Auto generate
                    projectId = currentProjectId,
                    name = pointName,
                    northing = lat,
                    easting = lng,
                    ellipsoidalHeight = alt,
                    orthoHeight = alt,
                    latDeg = lat,
                    lonDeg = lng,
                    fixType = fixType?.name,
                    description = "Hızlı alım noktası",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                pointDao.upsert(point)

                runOnUiThread {
                    Toast.makeText(this@QuickPointSurveyActivity, "Nokta '$pointName' kaydedildi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@QuickPointSurveyActivity, "Nokta kaydetme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickPointSurveyScreen(onBack: () -> Unit, onSavePoint: (String, Double, Double, Double?, FixType?) -> Unit) {
    var isRecording by remember { mutableStateOf(false) }
    var pointCount by remember { mutableStateOf(0) }
    var pointNamePrefix by remember { mutableStateOf("QP") }

    // GNSS konumu al
    val pos by GnssPositionRepository.position.collectAsState()
    val lat = pos.lat
    val lng = pos.lon
    val alt = pos.alt
    val fix = pos.fixType
    val sats = pos.satsInUse
    val hdop = pos.hdop

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hızlı Alım") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Durum kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null)
                        Text(
                            "Hızlı Nokta Alımı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Hızlı ve pratik nokta ölçümü için kullanın",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Nokta adı prefix girişi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Nokta Adı Ayarları",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = pointNamePrefix,
                        onValueChange = { pointNamePrefix = it },
                        label = { Text("Nokta Öneki") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        "Sonraki nokta: ${pointNamePrefix}${String.format("%03d", pointCount + 1)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // GPS durumu
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "GPS Durumu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fix Durumu:")
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(when(fix) {
                                    FixType.RTK_FIX -> "RTK FIX"
                                    FixType.RTK_FLOAT -> "RTK FLOAT"
                                    FixType.DGPS -> "DGPS"
                                    FixType.SINGLE -> "GPS 3D"
                                    else -> "YOK"
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
                        Text("Uydu Sayısı:")
                        Text("${sats ?: 0}", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("HDOP:")
                        Text("${hdop?.let { "%.2f".format(it) } ?: "---"}", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Mevcut konum
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Mevcut Konum",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enlem:")
                        Text("${lat?.let { "%.8f".format(it) } ?: "---"}", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Boylam:")
                        Text("${lng?.let { "%.8f".format(it) } ?: "---"}", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Yükseklik:")
                        Text("${alt?.let { "%.3f m".format(it) } ?: "--- m"}", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Alınan Nokta:")
                        Text("$pointCount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Kontrol butonları
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRecording)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                // Kayıt durduruluyor - nokta kaydet
                                if (lat != null && lng != null) {
                                    val pointName = "${pointNamePrefix}${String.format("%03d", pointCount + 1)}"
                                    onSavePoint(pointName, lat, lng, alt, fix)
                                    pointCount++
                                }
                                isRecording = false
                            } else {
                                // Kayıt başlatılıyor
                                isRecording = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        ),
                        enabled = lat != null && lng != null && (fix == FixType.RTK_FIX || fix == FixType.RTK_FLOAT)
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRecording) "Noktayı Kaydet" else "Nokta Al")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Tek nokta alımı
                                if (lat != null && lng != null) {
                                    val pointName = "${pointNamePrefix}${String.format("%03d", pointCount + 1)}"
                                    onSavePoint(pointName, lat, lng, alt, fix)
                                    pointCount++
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isRecording && lat != null && lng != null && (fix == FixType.RTK_FIX || fix == FixType.RTK_FLOAT)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Hızlı Al")
                        }

                        OutlinedButton(
                            onClick = {
                                pointCount = 0
                                isRecording = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Sıfırla")
                        }
                    }

                    // GPS durumu uyarısı
                    if (fix != FixType.RTK_FIX && fix != FixType.RTK_FLOAT) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "RTK sinyali bekleniyor. Nokta alımı için RTK FIX/FLOAT gereklidir.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            if (isRecording) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Nokta ölçümü devam ediyor...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Cihazı sabit tutun ve \"Noktayı Kaydet\" butonuna basın",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
