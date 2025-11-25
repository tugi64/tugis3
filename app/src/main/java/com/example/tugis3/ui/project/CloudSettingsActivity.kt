package com.example.tugis3.ui.project

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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CloudSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CloudSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudSettingsScreen(onBack: () -> Unit) {
    var useCloud by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var autoSync by remember { mutableStateOf(false) }
    var syncInterval by remember { mutableStateOf("15") }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulut Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bulut Kullanımı
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (useCloud)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Bulut Hizmeti Kullan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Veri senkronizasyonu ve koordinat paylaşımı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useCloud,
                            onCheckedChange = { useCloud = it }
                        )
                    }
                }
            }

            if (useCloud) {
                // Sunucu Bilgileri
                item {
                    Text(
                        "Sunucu Bilgileri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Sunucu URL") },
                        placeholder = { Text("https://cloud.example.com") },
                        leadingIcon = { Icon(Icons.Filled.Cloud, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Kullanıcı Adı") },
                        placeholder = { Text("kullanici@example.com") },
                        leadingIcon = { Icon(Icons.Filled.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Şifre") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Senkronizasyon Ayarları
                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Senkronizasyon Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Otomatik Senkronizasyon")
                                Text(
                                    "Belirli aralıklarla otomatik sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoSync,
                                onCheckedChange = { autoSync = it }
                            )
                        }
                    }
                }

                if (autoSync) {
                    item {
                        OutlinedTextField(
                            value = syncInterval,
                            onValueChange = { syncInterval = it },
                            label = { Text("Senkronizasyon Aralığı (dakika)") },
                            placeholder = { Text("15") },
                            leadingIcon = { Icon(Icons.Filled.Timer, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Bulut Özellikleri
                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bulut Özellikleri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CloudFeatureItem(
                                Icons.Filled.CloudUpload,
                                "Koordinat Sistemi Paylaşımı",
                                "Koordinat parametrelerini buluttan kullan"
                            )
                            HorizontalDivider()
                            CloudFeatureItem(
                                Icons.Filled.Sync,
                                "Proje Senkronizasyonu",
                                "Projelerinizi birden fazla cihazda kullanın"
                            )
                            HorizontalDivider()
                            CloudFeatureItem(
                                Icons.Filled.Backup,
                                "Otomatik Yedekleme",
                                "Verileriniz güvenle bulutta saklanır"
                            )
                            HorizontalDivider()
                            CloudFeatureItem(
                                Icons.Filled.People,
                                "Takım Çalışması",
                                "Ekip üyeleriyle veri paylaşımı"
                            )
                        }
                    }
                }

                // Bağlantı Test Butonu
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                snackbar.showSnackbar("Sunucu bağlantısı test ediliyor...")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = serverUrl.isNotBlank() && username.isNotBlank()
                    ) {
                        Icon(Icons.Filled.CloudDone, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bağlantıyı Test Et")
                    }
                }

                // Kaydet Butonu
                item {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                snackbar.showSnackbar("Bulut ayarları kaydedildi")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ayarları Kaydet")
                    }
                }
            } else {
                // Bulut Kapalı Bilgi
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Filled.CloudOff,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Bulut Hizmeti Kapalı",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Bulut özelliklerini kullanmak için yukarıdaki anahtarı açın",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bilgi Notu
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column {
                            Text(
                                "Özelleştirilmiş Özellik",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Bu özellik özelleştirilebilir. Gerekirse bizimle iletişime geçin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

