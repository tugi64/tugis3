@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.project

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
class AreasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                AreasScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AreasScreen(onBack: () -> Unit) {
    var isCloudConnected by remember { mutableStateOf(true) }
    var syncProgress by remember { mutableStateOf(0.65f) }
    var autoSync by remember { mutableStateOf(true) }
    var cloudStorage by remember { mutableStateOf("15.2 GB / 50 GB") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulut Ayarları") },
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
            // Bulut Bağlantı Durumu
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCloudConnected)
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
                            if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isCloudConnected) Color.Green else Color.Red
                        )
                        Text(
                            if (isCloudConnected) "Bulut Bağlantısı Aktif" else "Bulut Bağlantısı Yok",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isCloudConnected) {
                        Text("Sunucu: TUGIS Cloud Server")
                        Text("Son Senkronizasyon: 13.10.2025 14:30")
                        Text("Depolama Kullanımı: $cloudStorage")

                        LinearProgressIndicator(
                            progress = 0.304f, // 15.2/50
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Senkronizasyon Ayarları
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Senkronizasyon Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Otomatik Senkronizasyon")
                        Switch(
                            checked = autoSync,
                            onCheckedChange = { autoSync = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Senkronizasyon Sıklığı")
                        Text("Her 30 dakika", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sadece Wi-Fi'de Senkronize Et")
                        Switch(
                            checked = true,
                            onCheckedChange = { }
                        )
                    }
                }
            }

            // Mevcut Senkronizasyon
            if (syncProgress > 0 && syncProgress < 1) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Senkronizasyon Devam Ediyor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        LinearProgressIndicator(
                            progress = syncProgress,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("${(syncProgress * 100).toInt()}% tamamlandı")
                        Text("Kalan süre: 2 dakika", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Bulut İşlemleri
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Bulut İşlemleri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { syncProgress = 0.2f },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isCloudConnected
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manuel Senkronizasyon")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Yedekle */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Yedekle")
                        }

                        OutlinedButton(
                            onClick = { /* Geri yükle */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Geri Yükle")
                        }
                    }
                }
            }

            // Hesap Bilgileri
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Hesap Bilgileri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kullanıcı:")
                        Text("tugis_user@example.com", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Plan:")
                        Text("Professional", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Son Güncelleme:")
                        Text("13.10.2025 14:30", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
