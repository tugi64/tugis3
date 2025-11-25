package com.example.tugis3.ui.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.model.CadOption
import com.example.tugis3.model.GnssReceivingMode
import com.example.tugis3.ui.device.DeviceSettingsDialog
import com.example.tugis3.ui.device.GnssDeviceViewModel
import kotlin.random.Random

@Composable
fun GnssReceivingScreen(
    viewModel: GnssDeviceViewModel = hiltViewModel()
) {
    val currentMode by viewModel.currentReceivingMode.collectAsState()
    val deviceSettings by viewModel.deviceSettings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val currentEpoch by viewModel.currentEpoch.collectAsState()
    val sessionTimer by viewModel.sessionTimer.collectAsState()
    val hrmsValue by viewModel.hrmsValue.collectAsState()
    val vrmsValue by viewModel.vrmsValue.collectAsState()
    val pdopValue by viewModel.pdopValue.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedCadOption by remember { mutableStateOf<CadOption?>(null) }

    // Simüle edilmiş GNSS değerler - Grafik alım ekranı benzeri
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateRealTimeValues(
                hrms = kotlin.random.Random.nextDouble(0.01, 0.1),
                vrms = kotlin.random.Random.nextDouble(0.01, 0.15),
                pdop = kotlin.random.Random.nextDouble(1.0, 4.0)
            )
            kotlinx.coroutines.delay(1000)
        }
    }

    // Ana ekran düzeni - Grafik Alım benzeri
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Üst bilgi paneli - Alet yüksekliği ve koordinat bilgileri
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Başlık
                Text(
                    text = "GNSS Ölçüm Sistemi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Koordinat bilgileri satırları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Kuzey koordinatı
                    MeasurementItem(
                        label = "KUZEY (N)",
                        value = "4545123.456",
                        unit = "m",
                        icon = Icons.Default.North
                    )

                    // Doğu koordinatı
                    MeasurementItem(
                        label = "DOĞU (E)",
                        value = "567890.123",
                        unit = "m",
                        icon = Icons.Default.East
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Alet yüksekliği
                    MeasurementItem(
                        label = "ALET YÜKSEKLİĞİ",
                        value = deviceSettings.antennaHeight.toString(),
                        unit = "m",
                        icon = Icons.Default.Height
                    )

                    // Elipsoidal yükseklik
                    MeasurementItem(
                        label = "ELİPSOİDAL H.",
                        value = "345.678",
                        unit = "m",
                        icon = Icons.Default.Terrain
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kalite göstergeleri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QualityIndicator(
                        label = "HRMS",
                        value = String.format("%.3f", hrmsValue),
                        unit = "m",
                        color = if (hrmsValue < 0.05) Color(0xFF4CAF50) else Color(0xFFFFC107)
                    )

                    QualityIndicator(
                        label = "VRMS",
                        value = String.format("%.3f", vrmsValue),
                        unit = "m",
                        color = if (vrmsValue < 0.08) Color(0xFF4CAF50) else Color(0xFFFFC107)
                    )

                    QualityIndicator(
                        label = "PDOP",
                        value = String.format("%.2f", pdopValue),
                        unit = "",
                        color = if (pdopValue < 2.0) Color(0xFF4CAF50) else Color(0xFFFFC107)
                    )
                }
            }
        }

        // Durum bilgileri paneli
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusItem(
                        label = "MOD",
                        value = currentMode.name,
                        color = Color(0xFF2196F3)
                    )

                    StatusItem(
                        label = "EPOCH",
                        value = currentEpoch.toString(),
                        color = Color(0xFF9C27B0)
                    )

                    StatusItem(
                        label = "SÜRE",
                        value = formatTime(sessionTimer),
                        color = Color(0xFF607D8B)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusItem(
                        label = "UYDU",
                        value = "12",
                        color = Color(0xFF4CAF50)
                    )

                    StatusItem(
                        label = "RTK",
                        value = "FIX",
                        color = Color(0xFF4CAF50)
                    )

                    StatusItem(
                        label = "BAT",
                        value = "85%",
                        color = Color(0xFF8BC34A)
                    )
                }
            }
        }

        // Kontrol butonları
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Ölçüm başlat/durdur butonu
            Button(
                onClick = {
                    if (isRecording) viewModel.stopRecording()
                    else viewModel.startRecording()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .padding(end = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFE53935) else Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (isRecording) "DURDUR" else "BAŞLAT",
                    fontWeight = FontWeight.Bold
                )
            }

            // Ayarlar butonu
            Button(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .weight(0.5f)
                    .height(56.dp)
                    .padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Alt bilgi çubuğu
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "© TUGIS v3.0",
                    color = Color.White,
                    fontSize = 14.sp
                )

                Text(
                    text = "RTK Aktif",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Ayarlar dialog'u
    if (showSettingsDialog) {
        DeviceSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            viewModel = viewModel
        )
    }
}

// Ölçüm değeri bileşeni
@Composable
private fun MeasurementItem(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "$value $unit",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Kalite göstergesi bileşeni
@Composable
private fun QualityIndicator(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )

        Text(
            text = "$value $unit",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Durum bilgi bileşeni
@Composable
private fun StatusItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Zaman formatlayıcı
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
