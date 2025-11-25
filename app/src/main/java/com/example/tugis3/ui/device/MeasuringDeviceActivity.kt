@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.device

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.gnss.model.FixType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MeasuringDeviceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                MeasuringDeviceScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasuringDeviceScreen(onBack: () -> Unit) {
    var roverMode by remember { mutableStateOf("Kinematik RTK") }
    var measurementInterval by remember { mutableStateOf("1 saniye") }
    var recordingMode by remember { mutableStateOf(false) }

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
                title = { Text("Gezici Ayarları") },
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
            // Gezici Durumu
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when(fix) {
                        FixType.RTK_FIX -> MaterialTheme.colorScheme.primaryContainer
                        FixType.RTK_FLOAT -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.DirectionsWalk,
                            contentDescription = null
                        )
                        Text(
                            "Gezici Durumu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
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

            // Gezici Modu
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Gezici Modu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Çalışma Modu")
                        AssistChip(
                            onClick = { },
                            label = { Text(roverMode) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ölçüm Aralığı")
                        Text(measurementInterval, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Otomatik Kayıt")
                        Switch(
                            checked = recordingMode,
                            onCheckedChange = { recordingMode = it }
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
                }
            }

            // Kontrol Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Gezici başlat */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Başlat")
                }

                OutlinedButton(
                    onClick = { /* Gezici durdur */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Durdur")
                }
            }
        }
    }
}
