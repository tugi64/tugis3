package com.example.tugis3.ui.device

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                DeviceInfoScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(onBackPressed: () -> Unit) {
    // Simulated device information
    val deviceInfo = remember {
        mapOf(
            "Cihaz Markası" to "eSurvey",
            "Model" to "E600 Pro",
            "Seri Numarası" to "E600351920028",
            "Firmware Versiyonu" to "v2.4.15",
            "Hardware Versiyonu" to "v1.2",
            "Bootloader Versiyonu" to "v1.0.3",
            "GNSS Çipleri" to "GPS+GLONASS+BeiDou+Galileo",
            "RTK Motoru" to "eSurvey RTK v3.2",
            "IMU Sensörü" to "9-Axis IMU",
            "Bluetooth Versiyonu" to "5.0",
            "Wi-Fi" to "802.11 b/g/n",
            "Batarya Kapasitesi" to "7200 mAh",
            "Çalışma Sıcaklığı" to "-40°C ~ +65°C",
            "IP Sınıfı" to "IP67",
            "Anten Tipi" to "Dual-Band GNSS",
            "Radyo Modülü" to "UHF 410-470 MHz",
            "Ürün Tarihi" to "2023-08-15",
            "Kalibrasyon Tarihi" to "2023-08-20",
            "Son Güncelleme" to "2024-01-15",
            "Toplam Çalışma" to "1247 saat"
        )
    }

    var batteryLevel by remember { mutableStateOf(78) }
    var isCharging by remember { mutableStateOf(false) }
    var temperature by remember { mutableStateOf(35.2) }
    var memoryUsage by remember { mutableStateOf(42) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cihaz Bilgisi") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Bilgileri yenile */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Cihaz Durumu
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Cihaz Durumu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Aktif ve Çalışıyor", style = MaterialTheme.typography.bodyMedium)
                            }

                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Battery Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isCharging) Icons.Default.BatteryChargingFull
                                    else when {
                                        batteryLevel > 80 -> Icons.Default.BatteryFull
                                        batteryLevel > 60 -> Icons.Default.Battery6Bar
                                        batteryLevel > 40 -> Icons.Default.Battery4Bar
                                        batteryLevel > 20 -> Icons.Default.Battery2Bar
                                        else -> Icons.Default.BatteryAlert
                                    },
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Batarya: $batteryLevel%")
                            }

                            Text("Sıcaklık: ${String.format("%.1f", temperature)}°C")
                        }

                        Spacer(Modifier.height(8.dp))

                        // Memory Usage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bellek Kullanımı: $memoryUsage%")
                            Text("Uptime: 25 gün 14 saat")
                        }

                        Spacer(Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = batteryLevel / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                // Donanım Bilgileri
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Donanım Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        val hardwareItems = listOf(
                            "Cihaz Markası", "Model", "Seri Numarası", "Hardware Versiyonu",
                            "GNSS Çipleri", "IMU Sensörü", "Anten Tipi", "Radyo Modülü",
                            "Bluetooth Versiyonu", "Wi-Fi", "IP Sınıfı", "Çalışma Sıcaklığı"
                        )

                        hardwareItems.forEach { key ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$key:",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = deviceInfo[key] ?: "Bilinmiyor",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (key != hardwareItems.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            item {
                // Yazılım Bilgileri
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Yazılım Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        val softwareItems = listOf(
                            "Firmware Versiyonu", "Bootloader Versiyonu", "RTK Motoru",
                            "Son Güncelleme"
                        )

                        softwareItems.forEach { key ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$key:",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = deviceInfo[key] ?: "Bilinmiyor",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (key != softwareItems.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            item {
                // Kalibrasyon ve Bakım
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Kalibrasyon ve Bakım", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kalibrasyon Tarihi:")
                            Text(deviceInfo["Kalibrasyon Tarihi"] ?: "Bilinmiyor", fontWeight = FontWeight.Medium)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Toplam Çalışma:")
                            Text(deviceInfo["Toplam Çalışma"] ?: "Bilinmiyor", fontWeight = FontWeight.Medium)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Batarya Kapasitesi:")
                            Text(deviceInfo["Batarya Kapasitesi"] ?: "Bilinmiyor", fontWeight = FontWeight.Medium)
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* Fabrika ayarları */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.RestartAlt, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Fabrika Ayarları")
                            }

                            OutlinedButton(
                                onClick = { /* Kalibrasyon test */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Tune, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Kalibrasyon Test")
                            }
                        }
                    }
                }
            }

            item {
                // Bağlantı Durumu
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bağlantı Durumu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        val connectionStatus = listOf(
                            "Bluetooth" to "Bağlı (TUGIS3_APP)",
                            "Wi-Fi" to "Bağlı (TugisNet_5G)",
                            "NTRIP" to "Aktif (cors.harita.gov.tr)",
                            "RTK Radyo" to "Standby",
                            "IMU" to "Kalibre ve Aktif"
                        )

                        connectionStatus.forEach { (service, status) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(service)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when {
                                            status.contains("Bağlı") || status.contains("Aktif") || status.contains("Kalibre") -> Icons.Default.CheckCircle
                                            status.contains("Standby") -> Icons.Default.Schedule
                                            else -> Icons.Default.Error
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            status.contains("Bağlı") || status.contains("Aktif") || status.contains("Kalibre") -> MaterialTheme.colorScheme.primary
                                            status.contains("Standby") -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.error
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(status, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Firmware güncelle */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SystemUpdate, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Firmware Güncelle")
                    }

                    Button(
                        onClick = { /* Cihazı yeniden başlat */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Yeniden Başlat")
                    }
                }
            }
        }
    }
}
