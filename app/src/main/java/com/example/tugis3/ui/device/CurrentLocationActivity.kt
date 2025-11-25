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
class CurrentLocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CurrentLocationScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentLocationScreen(onBack: () -> Unit) {
    // GNSS konumu al
    val pos by GnssPositionRepository.position.collectAsState()
    val lat = pos.lat
    val lng = pos.lon
    val alt = pos.alt
    val fix = pos.fixType
    val sats = pos.satsInUse
    val hdop = pos.hdop

    var isTracking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mevcut Durum") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { isTracking = !isTracking }) {
                        Icon(
                            if (isTracking) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = if (isTracking) "Durdur" else "Başlat"
                        )
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
            // Anlık Konum
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
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Text(
                            "Anlık Konum",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isTracking) {
                            Icon(Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enlem:")
                        Text("${lat?.let { "%.8f°".format(it) } ?: "---"}", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Boylam:")
                        Text("${lng?.let { "%.8f°".format(it) } ?: "---"}", fontWeight = FontWeight.Medium)
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

            // GPS Kalitesi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "GPS Kalitesi",
                        style = MaterialTheme.typography.titleMedium,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Son Güncelleme:")
                        Text("Şimdi", fontWeight = FontWeight.Medium, color = Color.Green)
                    }
                }
            }

            // Koordinat Formatları
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Koordinat Formatları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("DD (Decimal):")
                        Text("${lat?.let { "%.6f".format(it) } ?: "---"}, ${lng?.let { "%.6f".format(it) } ?: "---"}")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("UTM:")
                        Text("36N 543215 4567890", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MGRS:")
                        Text("36TXL 43215 67890", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Kontrol Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Konumu kaydet */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Konumu Kaydet")
                }

                OutlinedButton(
                    onClick = { /* Konumu paylaş */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Paylaş")
                }
            }
        }
    }
}
