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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProtectionUpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                ProtectionUpdateScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProtectionUpdateScreen(onBack: () -> Unit) {
    var updateProgress by remember { mutableStateOf(0f) }
    var isUpdating by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf("13.10.2025 14:30") }

    // GNSS konumu al
    val pos by GnssPositionRepository.position.collectAsState()
    val lat = pos.lat
    val lng = pos.lon
    val alt = pos.alt

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konum Güncelle") },
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
            // Güncelleme Durumu
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUpdating)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
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
                            if (isUpdating) Icons.Default.CloudSync else Icons.Default.CloudDone,
                            contentDescription = null
                        )
                        Text(
                            if (isUpdating) "Konum Güncelleniyor" else "Konum Güncel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isUpdating) {
                        LinearProgressIndicator(
                            progress = updateProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${(updateProgress * 100).toInt()}% tamamlandı")
                    } else {
                        Text("Son Güncelleme: $lastUpdateTime")
                    }
                }
            }

            // Mevcut Konum Bilgisi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Konum Kalitesi:")
                        Text("Yüksek", fontWeight = FontWeight.Medium, color = Color.Green)
                    }
                }
            }

            // Güncelleme Ayarları
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Güncelleme Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Otomatik Güncelleme")
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
                        Text("Güncelleme Sıklığı")
                        Text("Her 5 dakika", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Minimum Hassasiyet")
                        Text("1 metre", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Arka Plan Güncelleme")
                        Switch(
                            checked = false,
                            onCheckedChange = { }
                        )
                    }
                }
            }

            // Güncelleme Geçmişi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Son Güncellemeler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("13.10.2025 14:30")
                        Text("Başarılı", color = Color.Green)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("13.10.2025 14:25")
                        Text("Başarılı", color = Color.Green)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("13.10.2025 14:20")
                        Text("Hata", color = Color.Red)
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
                        isUpdating = !isUpdating
                        if (isUpdating) {
                            updateProgress = 0.7f
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Manuel Güncelle")
                }

                OutlinedButton(
                    onClick = { /* Konum sıfırla */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sıfırla")
                }
            }
        }
    }
}
