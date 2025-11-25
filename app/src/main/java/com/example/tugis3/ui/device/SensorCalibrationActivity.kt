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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SensorCalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                SensorCalibrationScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SensorCalibrationScreen(onBack: () -> Unit) {
    var calibrationProgress by remember { mutableStateOf(0f) }
    var isCalibrating by remember { mutableStateOf(false) }
    var compassCalibrated by remember { mutableStateOf(true) }
    var gyroscopeCalibrated by remember { mutableStateOf(false) }
    var accelerometerCalibrated by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensör Kalibre") },
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
            // Kalibrasyon Durumu
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCalibrating)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else if (compassCalibrated && gyroscopeCalibrated && accelerometerCalibrated)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
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
                            if (isCalibrating) Icons.Default.Tune else Icons.Default.Sensors,
                            contentDescription = null
                        )
                        Text(
                            if (isCalibrating) "Kalibrasyon Devam Ediyor"
                            else if (compassCalibrated && gyroscopeCalibrated && accelerometerCalibrated) "Tüm Sensörler Kalibre"
                            else "Kalibrasyon Gerekli",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isCalibrating) {
                        LinearProgressIndicator(
                            progress = calibrationProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${(calibrationProgress * 100).toInt()}% tamamlandı")
                    }
                }
            }

            // Sensör Durumları
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Sensör Durumları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SensorStatusRow("Pusula", compassCalibrated)
                    SensorStatusRow("Jiroskop", gyroscopeCalibrated)
                    SensorStatusRow("İvmeölçer", accelerometerCalibrated)
                }
            }

            // Kalibrasyon Ayarları
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Kalibrasyon Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Otomatik Kalibrasyon")
                        Switch(
                            checked = true,
                            onCheckedChange = { }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hassasiyet Seviyesi")
                        Text("Yüksek", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kalibrasyon Süresi")
                        Text("60 saniye", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Son Kalibrasyon Bilgisi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Son Kalibrasyon Bilgileri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tarih:")
                        Text("13.10.2025 10:30", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Süre:")
                        Text("1 dakika 23 saniye", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sonuç:")
                        Text("Başarılı", fontWeight = FontWeight.Medium, color = Color.Green)
                    }
                }
            }

            // Kontrol Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isCalibrating = !isCalibrating
                        if (isCalibrating) {
                            calibrationProgress = 0.4f
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isCalibrating
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kalibrasyon Başlat")
                }

                OutlinedButton(
                    onClick = {
                        isCalibrating = false
                        calibrationProgress = 0f
                        gyroscopeCalibrated = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sıfırla")
                }
            }
        }
    }
}

@Composable
private fun SensorStatusRow(
    sensorName: String,
    isCalibrated: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(sensorName)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isCalibrated) Color.Green else Color.Red,
                modifier = Modifier.size(20.dp)
            )
            Text(
                if (isCalibrated) "Kalibre" else "Gerekli",
                fontWeight = FontWeight.Medium,
                color = if (isCalibrated) Color.Green else Color.Red
            )
        }
    }
}
