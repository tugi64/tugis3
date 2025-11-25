package com.example.tugis3.ui.tools

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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class StationRenewalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                StationRenewalScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationRenewalScreen(onBack: () -> Unit) {
    var renewalMethod by remember { mutableStateOf("Otomatik") }
    var isRenewing by remember { mutableStateOf(false) }
    var renewalProgress by remember { mutableStateOf(0f) }
    var lastRenewalTime by remember { mutableStateOf("--:--:--") }
    var autoRenewalInterval by remember { mutableStateOf(30f) }

    // Baz istasyonu bilgileri
    var baseLatitude by remember { mutableStateOf("40.123456°") }
    var baseLongitude by remember { mutableStateOf("29.654321°") }
    var baseHeight by remember { mutableStateOf("125.45 m") }
    var antennaHeight by remember { mutableStateOf("1.80 m") }

    // Pozisyonlama bilgileri
    var fixType by remember { mutableStateOf("Fixed RTK") }
    var satellites by remember { mutableStateOf(18) }
    var pdop by remember { mutableStateOf(1.2) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İstasyon Yenileme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Ayarlar */ }) {
                        Icon(Icons.Default.Settings, "Ayarlar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                Text("Mevcut İstasyon", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Base_Station_001", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Son Güncelleme: $lastRenewalTime", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(
                                Icons.Default.Wifi,
                                "İstasyon",
                                modifier = Modifier.size(48.dp),
                                tint = if (isRenewing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Yenileme Yöntemi", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val methods = listOf(
                            "Otomatik" to "Belirli aralıklarla otomatik yenileme yapar",
                            "Manuel" to "Kullanıcı tarafından manuel olarak başlatılır",
                            "Zamanlı" to "Belirtilen zamanlarda otomatik yenileme yapar"
                        )

                        methods.forEach { (method, desc) ->
                            FilterChip(
                                selected = renewalMethod == method,
                                onClick = { renewalMethod = method },
                                label = {
                                    Column {
                                        Text(method, fontWeight = FontWeight.Bold)
                                        Text(desc, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }

                        if (renewalMethod == "Otomatik") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Yenileme Aralığı: ${autoRenewalInterval.toInt()} dakika", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = autoRenewalInterval,
                                onValueChange = { autoRenewalInterval = it },
                                valueRange = 5f..120f,
                                steps = 22
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("5 dk", style = MaterialTheme.typography.bodySmall)
                                Text("120 dk", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("İstasyon Bilgileri", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRowItem("Enlem", baseLatitude)
                        InfoRowItem("Boylam", baseLongitude)
                        InfoRowItem("Elipsoidal Yükseklik", baseHeight)
                        InfoRowItem("Anten Yüksekliği", antennaHeight)

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRowItem("Fix Tipi", fixType)
                        InfoRowItem("Uydu Sayısı", "$satellites")
                        InfoRowItem("PDOP", String.format("%.1f", pdop))
                    }
                }
            }

            if (isRenewing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    progress = renewalProgress,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Yenileme Durumu", style = MaterialTheme.typography.titleMedium)
                                    Text("Konum güncelleniyor...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = renewalProgress,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "İlerleme: ${(renewalProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRenewing) {
                        OutlinedButton(
                            onClick = {
                                isRenewing = false
                                renewalProgress = 0f
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, "Durdur")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Durdur")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { /* Geçmiş */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.History, "Geçmiş")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Geçmiş")
                        }

                        Button(
                            onClick = {
                                isRenewing = true
                                // Simülasyon için progress güncelleme
                                lastRenewalTime = java.time.LocalTime.now().format(
                                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, "Yenile")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yenile")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Yenileme Geçmişi", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        repeat(3) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Yenileme ${3 - index}", style = MaterialTheme.typography.bodyMedium)
                                    Text("15:${30 + index * 5}:${20 + index}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Başarılı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            if (index < 2) Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Icon(Icons.Default.Info, "Bilgi", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "İstasyon yenileme, baz istasyonunun konumunu yeniden hesaplayarak daha doğru düzeltme verileri sağlar. Uzun süreli statik ölçümlerde ve CORS bağlantı kesintilerinde kullanılır.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    // Simülasyon için progress güncelleme
    LaunchedEffect(isRenewing) {
        if (isRenewing) {
            while (renewalProgress < 1f && isRenewing) {
                kotlinx.coroutines.delay(100)
                renewalProgress += 0.02f
                if (renewalProgress >= 1f) {
                    isRenewing = false
                    renewalProgress = 0f
                }
            }
        }
    }
}

@Composable
private fun InfoRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
