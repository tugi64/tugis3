package com.example.tugis3.ui.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.ui.device.DeviceSettingsDialog
import com.example.tugis3.ui.device.GnssDeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointSurveyScreen(
    viewModel: GnssDeviceViewModel = hiltViewModel()
) {
    val deviceSettings by viewModel.deviceSettings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val currentEpoch by viewModel.currentEpoch.collectAsState()
    val sessionTimer by viewModel.sessionTimer.collectAsState()
    val hrmsValue by viewModel.hrmsValue.collectAsState()
    val vrmsValue by viewModel.vrmsValue.collectAsState()
    val pdopValue by viewModel.pdopValue.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var pointName by remember { mutableStateOf("P01") }
    var pointCode by remember { mutableStateOf("") }
    var pointDescription by remember { mutableStateOf("") }
    var showPointListDialog by remember { mutableStateOf(false) }

    // Simüle edilmiş GNSS değerler
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

    // Ana ekran düzeni - Nokta Ölçüm odaklı
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Başlık paneli
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOKTA ÖLÇÜM SİSTEMİ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "RTK GNSS Nokta Alımı",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sol panel - Nokta bilgileri ve giriş
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "NOKTA BİLGİLERİ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Nokta adı girişi
                    OutlinedTextField(
                        value = pointName,
                        onValueChange = { pointName = it },
                        label = { Text("Nokta Adı") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        }
                    )

                    // Nokta kodu girişi
                    OutlinedTextField(
                        value = pointCode,
                        onValueChange = { pointCode = it },
                        label = { Text("Nokta Kodu") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null)
                        }
                    )

                    // Açıklama girişi
                    OutlinedTextField(
                        value = pointDescription,
                        onValueChange = { pointDescription = it },
                        label = { Text("Açıklama") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        maxLines = 2,
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Koordinat bilgileri
                    Text(
                        text = "ANLIK KOORDİNATLAR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    CoordinateInfo(
                        label = "KUZEY (Y)",
                        value = "4545123.456",
                        unit = "m"
                    )

                    CoordinateInfo(
                        label = "DOĞU (X)",
                        value = "567890.123",
                        unit = "m"
                    )

                    CoordinateInfo(
                        label = "YÜKSEKLİK (Z)",
                        value = "1131.004",
                        unit = "m"
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Nokta listesi butonu
                    Button(
                        onClick = { showPointListDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("NOKTA LİSTESİ")
                    }
                }
            }

            // Sağ panel - Kalite ve kontrol
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "KALİTE GÖSTERGELERİ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Kalite göstergeleri
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QualityMeter(
                            label = "HRMS",
                            value = String.format("%.3f", hrmsValue),
                            unit = "m",
                            color = if (hrmsValue < 0.05) Color(0xFF4CAF50) else Color(0xFFFFC107),
                            isGood = hrmsValue < 0.05
                        )

                        QualityMeter(
                            label = "VRMS",
                            value = String.format("%.3f", vrmsValue),
                            unit = "m",
                            color = if (vrmsValue < 0.08) Color(0xFF4CAF50) else Color(0xFFFFC107),
                            isGood = vrmsValue < 0.08
                        )

                        QualityMeter(
                            label = "PDOP",
                            value = String.format("%.2f", pdopValue),
                            unit = "",
                            color = if (pdopValue < 2.0) Color(0xFF4CAF50) else Color(0xFFFFC107),
                            isGood = pdopValue < 2.0
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Durum bilgileri
                    Text(
                        text = "DURUM BİLGİLERİ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusIndicator(
                            label = "RTK DURUM",
                            value = "FIX",
                            color = Color(0xFF4CAF50),
                            icon = Icons.Default.GpsFixed
                        )

                        StatusIndicator(
                            label = "UYDU",
                            value = "12",
                            color = Color(0xFF2196F3),
                            icon = Icons.Default.Satellite
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusIndicator(
                            label = "EPOCH",
                            value = currentEpoch.toString(),
                            color = Color(0xFF9C27B0),
                            icon = Icons.Default.Timer
                        )

                        StatusIndicator(
                            label = "SÜRE",
                            value = formatTime(sessionTimer),
                            color = Color(0xFF607D8B),
                            icon = Icons.Default.Schedule
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Alet yüksekliği
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ALET YÜKSEKLİĞİ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${deviceSettings.antennaHeight} m",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Ana ölçüm butonu
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRecording) Color(0xFFE53935) else Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isRecording) viewModel.stopRecording()
                                else viewModel.startRecording()
                            },
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = if (isRecording) "ÖLÇÜM DURDUR" else "ÖLÇÜM BAŞLAT",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ayarlar butonu
                    Button(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("AYARLAR")
                    }
                }
            }
        }
    }

    // Dialog'lar
    if (showSettingsDialog) {
        DeviceSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            viewModel = viewModel
        )
    }

    if (showPointListDialog) {
        PointListDialog(
            onDismiss = { showPointListDialog = false }
        )
    }
}

@Composable
private fun CoordinateInfo(
    label: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value $unit",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun QualityMeter(
    label: String,
    value: String,
    unit: String,
    color: Color,
    isGood: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Icon(
            imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(20.dp)
                .padding(bottom = 4.dp)
        )

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )

        Text(
            text = "$value $unit",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun StatusIndicator(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = label,
            fontSize = 10.sp,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointListDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nokta Listesi") },
        text = {
            Column {
                Text("Kaydedilen Noktalar:")
                Text("• P01 - Başlangıç noktası")
                Text("• P02 - Köşe noktası")
                Text("• P03 - Sınır noktası")
                Text("Toplam: 3 nokta")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("KAPAT")
            }
        },
        dismissButton = {
            TextButton(onClick = { }) {
                Text("DIŞA AKTAR")
            }
        }
    )
}

// Zaman formatlayıcı
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
